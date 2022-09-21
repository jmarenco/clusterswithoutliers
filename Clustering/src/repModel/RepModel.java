package repModel;

import java.util.ArrayList;

import general.*;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

public class RepModel
{
	// Instance
	private Instance _instance;
	
	// Solution
	private ArrayList<Cluster> _clusters;
	
	// Parameters
	private boolean _integer = true;
	private boolean _strongBinding = true;
	private int _maxTime = 3600;
	
	private static boolean _verbose = true;
	private static boolean _summary = false;
	
	// Model sizes
	private int p; // Points
	private int n; // Clusters
	private int o; // Outliers
	private int d; // Dimension

	// Solver
	private IloCplex cplex;

    // Variables
	private IloNumVar[][] x;
	private IloNumVar[][] r;
	private IloNumVar[][] l;

	public RepModel(Instance instance)
	{
		_instance = instance;

		p = _instance.getPoints();
		n = _instance.getClusters();
		o = _instance.getOutliers();
		d = _instance.getDimension();
	}
	
	public void setInteger(boolean value)
	{
		_integer = value;
	}
	
	public void setStrongBinding(boolean value)
	{
		_strongBinding = value;
	}
	
	public void setMaxTime(int value)
	{
		_maxTime = value;
	}
	
	public Solution solve() throws IloException
	{
		createSolver();
		createVariables();
		
	    createCoherenceConstraint();
	    createRepresentationConstraint();
	    createMaxClustersConstraint();
	    createOutliersConstraint();
		createBindingConstraints();
		createOrderingConstraints();
		createObjective();
		
		solveModel();
    	obtainSolution();
	    closeSolver();
	    
	    return new Solution(_clusters);
	}

	private void createSolver() throws IloException
	{
		cplex = new IloCplex();
		
		if( _verbose == false )
		{
			cplex.setOut(null);
			cplex.setWarning(null);
		}
	}

	private void createVariables() throws IloException
	{
		x = new IloNumVar[p][p];
		r = new IloNumVar[p][d];
		l = new IloNumVar[p][d];
		
		for(int i=0; i<p; ++i)
	    for(int j=i; j<p; ++j)
	    {
	    	x[i][j] = _integer ? cplex.boolVar("x" + i + "_" + j) : cplex.numVar(0, 1000, "x" + i + "_" + j);
	    }
		
	    for(int j=0; j<p; ++j)
		for(int t=0; t<d; ++t)
	    	r[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + j + "_" + t);

	    for(int j=0; j<p; ++j)
		for(int t=0; t<d; ++t)
	    	l[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + j + "_" + t);
	}

	private void createOutliersConstraint() throws IloException
	{
		IloNumExpr lhs = cplex.linearIntExpr();
		for(int i=0; i<p; ++i)
		    for(int j=i; j<p; ++j)
		    	lhs = cplex.sum(lhs, x[i][j]);

		cplex.addGe(lhs, p-o, "outliers");
	}

	private void createCoherenceConstraint() throws IloException
	{
		for(int i=0; i<p; ++i)
		for(int j=i+1; j<p; ++j)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, x[i][j]);
			lhs = cplex.sum(lhs, cplex.prod(-1, x[i][i]));
		    cplex.addLe(lhs, 0, "coh_" + i + "_" + j);
	    }
	}
	
	private void createRepresentationConstraint() throws IloException
	{
		for(int j=0; j<p; ++j)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			for (int i = 0; i <= j; i++) 
				lhs = cplex.sum(lhs, x[i][j]);
		    cplex.addLe(lhs, 1, "rep" + j);
	    }
	}

	private void createMaxClustersConstraint() throws IloException
	{
		IloNumExpr lhs = cplex.linearIntExpr();
	    for(int i=0; i<p; ++i)
	    	lhs = cplex.sum(lhs, x[i][i]);
	 	cplex.addLe(lhs, n);
	}

	private void createBindingConstraints() throws IloException
	{
		if( _strongBinding == true )
			createStrongBindingConstraints();
		else
//			createWeakBindingConstraints();
			throw new RuntimeException("Weak binding constraints not exist for POP formulation!");
	}

	private void createStrongBindingConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    for(int c=0; c<=i; ++c)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, l[c][t]);
			lhs = cplex.sum(lhs, cplex.prod(_instance.max(t) - _instance.getPoint(i).get(t), x[c][i]));
			cplex.addLe(lhs, _instance.max(t));
		}
	    
	    for(int i=0; i<p; ++i)
		for(int c=0; c<=i; ++c)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, r[c][t]);
			lhs = cplex.sum(lhs, cplex.prod(_instance.min(t) - _instance.getPoint(i).get(t), x[c][i]));
			cplex.addGe(lhs, _instance.min(t));
		}
	}
	
	private void createOrderingConstraints() throws IloException
	{
		for(int j=0; j<p; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(-1, r[j][t]));
			
			cplex.addLe(lhs, 0);
		}
	}

	
//	private void createWeakBindingConstraints() throws IloException
//	{
//		for(int i=0; i<p; ++i)
//	    for(int j=0; j<n; ++j)
//		for(int t=0; t<d; ++t)
//		{
//			IloNumExpr lhs = cplex.linearIntExpr();
//			lhs = cplex.sum(lhs, l[j][t]);
//			lhs = cplex.sum(lhs, cplex.prod(_instance.max(t) - _instance.min(t), z[i][j]));
//			cplex.addLe(lhs, _instance.getPoint(i).get(t) + _instance.max(t) - _instance.min(t));
//		}
//	    
//	    for(int i=0; i<p; ++i)
//	    for(int j=0; j<n; ++j)
//		for(int t=0; t<d; ++t)
//		{
//			IloNumExpr lhs = cplex.linearIntExpr();
//			lhs = cplex.sum(lhs, r[j][t]);
//			lhs = cplex.sum(lhs, cplex.prod(-_instance.max(t) + _instance.min(t), z[i][j]));
//			cplex.addGe(lhs, _instance.getPoint(i).get(t) - _instance.max(t) + _instance.min(t));
//		}
//	}	

	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int j=0; j<p; ++j)
		for(int t=0; t<d; ++t)
		{
			fobj = cplex.sum(fobj, cplex.prod(1.0, r[j][t]));
			fobj = cplex.sum(fobj, cplex.prod(-1.0, l[j][t]));
		}
		
		cplex.addMinimize(fobj);
	}
	
	private void solveModel() throws IloException
	{
		long start = System.currentTimeMillis();
		
		cplex.setParam(IntParam.TimeLimit, _maxTime);
		cplex.solve();
		
		double time = (System.currentTimeMillis() - start) / 1000.0;
		
		if( _summary == false)
		{
			System.out.println("Status: " + cplex.getStatus());
			System.out.println("Time: " + String.format("%6.2f", time));
			System.out.println("Nodes: " + cplex.getNnodes());
			System.out.println("Gap: " + ((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) : "  ****"));
			System.out.println("Cuts: " + cplex.getNcuts(IloCplex.CutType.User));
		}
		else
		{
			System.out.print(_instance.getName() + " | ");
			System.out.print(cplex.getStatus() + " | ");
			System.out.print("Obj: " + String.format("%6.4f", cplex.getObjValue()) + " | ");
			System.out.print(String.format("%6.2f", time) + " sec. | ");
			System.out.print(cplex.getNnodes() + " nodes | ");
			System.out.print(((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) + " % | ": "  **** | "));
			System.out.print(cplex.getNcuts(IloCplex.CutType.User) + " cuts | ");
			System.out.print("MR: " + 0 + " | ");
			System.out.print("SF: " + 0 + " | ");
//			System.out.print("Cut execs: " + separator.getExecutions() + " | ");
			System.out.print("    | ");
			System.out.print("MT: " + _maxTime + " | ");
//			System.out.print("SB: " + (_symmetryBreaking == SymmetryBreaking.Size ? "Size" : (_symmetryBreaking == SymmetryBreaking.IndexSum ? "Idx " : (_symmetryBreaking == SymmetryBreaking.OrderedStart ? "OrSt" : "    "))) + " |"); 
			System.out.println();
		}
	}

	private void obtainSolution() throws IloException, UnknownObjectException
	{
		_clusters = new ArrayList<Cluster>();
		
    	if( cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible )
		{
	    	for(int j=0; j<p; ++j)
	    	{
	    		RectangularCluster cluster = new RectangularCluster(d);
	    		
				for(int t=0; t<d; ++t)
				{
					cluster.setMin(t, cplex.getValue(l[j][t]));
					cluster.setMax(t, cplex.getValue(r[j][t]));
				}
	    		
	    		_clusters.add(cluster);
	    	}
	    		
			for(int i=0; i<p; ++i)
		    for(int j=0; j<=i; ++j) if( cplex.getValue(x[j][i]) > 0.5 ) // Point i is in cluster j
		    {
//		    	System.out.println("x" + j + "_" + i + " = " + cplex.getValue(x[j][i]));
		    	_clusters.get(j).add(_instance.getPoint(i));
		    }

//			for(int j=0; j<p; ++j)
//			for(int t=0; t<d; ++t)
//			{
//				System.out.println("l" + j + "_" + t + " = " + cplex.getValue(l[j][t]));
//				System.out.println("r" + j + "_" + t + " = " + cplex.getValue(r[j][t]));
//			}
		}
	}
	
	private void closeSolver()
	{
		cplex.end();
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public IloNumVar xVar(int i, int j)
	{
		return x[i][j];
	}
	
	public IloNumVar rVar(int cluster, int dimension)
	{
		return r[cluster][dimension];
	}
	
	public IloNumVar lVar(int cluster, int dimension)
	{
		return l[cluster][dimension];
	}
	
	public IloCplex getCplex()
	{
		return cplex;
	}
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}
}
