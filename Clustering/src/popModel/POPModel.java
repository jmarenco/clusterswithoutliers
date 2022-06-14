package popModel;

import java.util.ArrayList;

import general.*;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

public class POPModel
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
	private IloNumVar[][] z;
	private IloNumVar[][] y;
	private IloNumVar[][] r;
	private IloNumVar[][] l;

	public POPModel(Instance instance)
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
	    createOutliersConstraint();
	    createNullingConstraints();
	    createConsecutiveOnesConstraints();
	    createExclusiveConstraints();
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
		z = new IloNumVar[p][n+1];
		y = new IloNumVar[n+1][p];
		r = new IloNumVar[n][d];
		l = new IloNumVar[n][d];
		
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n+1; ++j)
	    {
	    	z[i][j] = _integer ? cplex.boolVar("z" + i + "_" + j) : cplex.numVar(0, 1000, "z" + i + "_" + j);
	    	y[j][i] = _integer ? cplex.boolVar("y" + j + "_" + i) : cplex.numVar(0, 1000, "y" + j + "_" + i);
	    }
		
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	r[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + j + "_" + t);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	l[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + j + "_" + t);
	}

	private void createOutliersConstraint() throws IloException
	{
		IloNumExpr lhs = cplex.linearIntExpr();
		for(int i=0; i<p; ++i)
			lhs = cplex.sum(lhs, z[i][1]);

		cplex.addLe(lhs, o, "outliers");
	}

	private void createNullingConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, y[n][i]);
		    cplex.addEq(lhs, 0, "null_y" + i);

			lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, z[i][0]);
		    cplex.addEq(lhs, 0, "null_z" + i);
	    }
	}
	
	private void createConsecutiveOnesConstraints() throws IloException
	{
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
		 	lhs = cplex.sum(lhs, y[j+1][i]);
			lhs = cplex.sum(lhs, cplex.prod(-1, y[j][i]));
		 	cplex.addLe(lhs, 0);
		 	
		 	lhs = cplex.linearIntExpr();
		 	lhs = cplex.sum(lhs, z[i][j]);
			lhs = cplex.sum(lhs, cplex.prod(-1, z[i][j+1]));
		 	cplex.addLe(lhs, 0);
		}
	}

	private void createExclusiveConstraints() throws IloException
	{
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
		 	lhs = cplex.sum(lhs, y[j][i]);
			lhs = cplex.sum(lhs, z[i][j+1]);
		 	cplex.addEq(lhs, 1);
		}
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
	    for(int j=1; j<n+1; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, l[j-1][t]);
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(t) - _instance.max(t), z[i][j]));
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(t) - _instance.max(t), y[j][i]));
			cplex.addLe(lhs, _instance.getPoint(i).get(t));
		}
	    
	    for(int i=0; i<p; ++i)
	    for(int j=1; j<n+1; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, r[j-1][t]);
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(t) - _instance.min(t), z[i][j]));
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(t) - _instance.min(t), y[j][i]));
			cplex.addGe(lhs, _instance.getPoint(i).get(t));
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

	private void createOrderingConstraints() throws IloException
	{
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(-1, r[j][t]));
			
			cplex.addLe(lhs, 0);
		}
	}

//	private void createSymmetryBreakingConstraints() throws IloException
//	{
//		if( _symmetryBreaking == SymmetryBreaking.Size )
//		{
//			for(int j=1; j<n; ++j)
//			{
//				IloNumExpr lhsOut = cplex.linearIntExpr();
//				
//			    for(int i=0; i<p; ++i)
//			    {
//				 	lhsOut = cplex.sum(lhsOut, z[i][j]);
//				 	lhsOut = cplex.sum(lhsOut, cplex.prod(-1, z[i][j-1]));
//			    }
//				    
//			    cplex.addGe(lhsOut, 0, "sb" + j);
//			}
//		}
//
//		if( _symmetryBreaking == SymmetryBreaking.IndexSum )
//		{
//			for(int j=1; j<n; ++j)
//			{
//				IloNumExpr lhsOut = cplex.linearIntExpr();
//				
//			    for(int i=0; i<p; ++i)
//			    {
//				 	lhsOut = cplex.sum(lhsOut, cplex.prod(i, z[i][j]));
//				 	lhsOut = cplex.sum(lhsOut, cplex.prod(-i, z[i][j-1]));
//			    }
//				    
//			    cplex.addGe(lhsOut, 0, "sb" + j);
//			}
//		}
//
//		if( _symmetryBreaking == SymmetryBreaking.OrderedStart )
//		{
//			for(int i=0; i<p; ++i)
//			for(int j=0; j<n-1; ++j)
//			for(int t=0; t<p; ++t) if( _instance.getPoint(t).get(0) < _instance.getPoint(i).get(0) )
//			{
//				IloNumExpr lhsOut = cplex.linearIntExpr();
//				lhsOut = cplex.sum(lhsOut, z[i][j]);
//				
//			    for(int k=0; k<p; ++k) if( _instance.getPoint(k).get(0) < _instance.getPoint(i).get(0) )
//				 	lhsOut = cplex.sum(lhsOut, cplex.prod(-1, z[k][j]));
//			    
//			    for(int jp=j+1; jp<n; ++jp)
//					lhsOut = cplex.sum(lhsOut, z[t][jp]);
//				    
//			    cplex.addLe(lhsOut, 1, "sb" + i + "_" + j + "_" + t);
//			}
//		}
//	}

	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int j=0; j<n; ++j)
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
	    	for(int j=0; j<n; ++j)
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
		    for(int j=1; j<n; ++j) if( cplex.getValue(z[i][j]) + cplex.getValue(y[j][i]) < 0.1 ) // Point i is in cluster j-1
		    {
//		    	System.out.println("z" + i + "_" + j + " = " + cplex.getValue(z[i][j]));
		    	_clusters.get(j-1).add(_instance.getPoint(i));
		    }

//			for(int j=0; j<n; ++j)
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
	
	public IloNumVar zVar(int point, int cluster)
	{
		return z[point][cluster];
	}
	
	public IloNumVar yVar(int cluster, int point)
	{
		return y[cluster][point];
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
