package clustering;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

public class RectangularModel
{
	// Instance
	private Instance _instance;
	
	// Solution
	private ArrayList<Cluster> _clusters;
	
	// Parameters
	private boolean _integer = true;
	private boolean _strongBinding = true;
	private int _maxTime = 3600;
	
	// Model sizes
	private int p; // Points
	private int n; // Clusters
	private int o; // Outliers
	private int d; // Dimension

	// Solver
	private IloCplex cplex;

    // Variables
	private IloNumVar[][] z;
	private IloNumVar[][] r;
	private IloNumVar[][] l;

	public RectangularModel(Instance instance)
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
	    createClusteringConstraints();
	    createBindingConstraints();
	    createOrderingConstraints();
		createOutliersConstraint();
		createObjective();
		
		solveModel();
    	obtainSolution();
	    closeSolver();
	    
	    return new Solution(_clusters);
	}

	private void createSolver() throws IloException
	{
		cplex = new IloCplex();
	}

	private void createVariables() throws IloException
	{
		z = new IloNumVar[p][n];
		r = new IloNumVar[n][d];
		l = new IloNumVar[n][d];
		
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = _integer ? cplex.boolVar("z" + i + "_" + j) : cplex.numVar(0, 1000, "z" + i + "_" + j);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	r[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + j + "_" + t);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	l[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + j + "_" + t);
	}

	private void createClusteringConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			
		    for(int j=0; j<n; ++j)
		    	lhs = cplex.sum(lhs, z[i][j]);
		    
		    cplex.addLe(lhs, 1, "clus" + i);
	    }
	}
	
	private void createBindingConstraints() throws IloException
	{
		if( _strongBinding == true )
			createStrongBindingConstraints();
		else
			createWeakBindingConstraints();
	}

	private void createStrongBindingConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(- _instance.getPoint(i).get(t) + _instance.max(t), z[i][j]));
			cplex.addLe(lhs, _instance.max(t));
		}
	    
	    for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, r[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(- _instance.getPoint(i).get(t) + _instance.min(t), z[i][j]));
			cplex.addGe(lhs, _instance.min(t));
		}
	}
	
	private void createWeakBindingConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(_instance.max(t) - _instance.min(t), z[i][j]));
			cplex.addLe(lhs, _instance.getPoint(i).get(t) + _instance.max(t) - _instance.min(t));
		}
	    
	    for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, r[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(-_instance.max(t) + _instance.min(t), z[i][j]));
			cplex.addGe(lhs, _instance.getPoint(i).get(t) - _instance.max(t) + _instance.min(t));
		}
	}	

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

	private void createOutliersConstraint() throws IloException
	{
		IloNumExpr lhsOut = cplex.linearIntExpr();
		
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		 	lhsOut = cplex.sum(lhsOut, z[i][j]);
		    
	    cplex.addGe(lhsOut, p-o, "out");
	}

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
		cplex.setParam(IntParam.TimeLimit, _maxTime);
		cplex.solve();
		
		System.out.println("Status: " + cplex.getStatus());
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
		    for(int j=0; j<n; ++j) if( cplex.getValue(z[i][j]) > 0.9 )
		    {
//		    	System.out.println("z" + i + "_" + j + " = " + cplex.getValue(z[i][j]));
		    	_clusters.get(j).add(_instance.getPoint(i));
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
}
