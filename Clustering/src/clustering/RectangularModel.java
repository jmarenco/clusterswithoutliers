package clustering;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;

public class RectangularModel
{
	private Instance _instance;
	private ArrayList<Cluster> _clusters;
	
	public RectangularModel(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution solve(boolean integer, double maxTime) throws IloException
	{
		// Model sizes
		int p = _instance.getPoints();
		int n = _instance.getClusters();
		int o = _instance.getOutliers();
		int d = _instance.getDimension();

		// Create solver
		IloCplex cplex = new IloCplex();

	    // Create variables
		IloNumVar[][] z = new IloNumVar[p][n];
		IloNumVar[][] r = new IloNumVar[n][d];
		IloNumVar[][] l = new IloNumVar[n][d];
		
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = integer ? cplex.boolVar("z" + i + "_" + j) : cplex.numVar(0, 1000, "z" + i + "_" + j);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	r[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + j + "_" + t);

	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
	    	l[j][t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + j + "_" + t);

	    // Create clustering constraints
	    for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			
		    for(int j=0; j<n; ++j)
		    	lhs = cplex.sum(lhs, z[i][j]);
		    
		    cplex.addLe(lhs, 1, "clus" + i);
	    }
	    
	    // Create binding constraints
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
	    
	    // Relation between l and r
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, l[j][t]);
			lhs = cplex.sum(lhs, cplex.prod(-1, r[j][t]));
			
			cplex.addLe(lhs, 0);
		}

	    // Create outliers constraint
		IloNumExpr lhsOut = cplex.linearIntExpr();
		
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		 	lhsOut = cplex.sum(lhsOut, z[i][j]);
		    
	    cplex.addGe(lhsOut, p-o, "out");
	    
	    // Create objective
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			fobj = cplex.sum(fobj, cplex.prod(1.0, r[j][t]));
			fobj = cplex.sum(fobj, cplex.prod(-1.0, l[j][t]));
		}
		
		cplex.addMinimize(fobj);
		
		// Solve model
		cplex.setParam(IntParam.TimeLimit, maxTime);
		cplex.solve();
		
		System.out.println("Status: " + cplex.getStatus());

		// Get solution
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
		    	System.out.println("z" + i + "_" + j + " = " + cplex.getValue(z[i][j]));
		    	_clusters.get(j).add(_instance.getPoint(i));
		    }

			for(int j=0; j<n; ++j)
			for(int t=0; t<d; ++t)
			{
				System.out.println("l" + j + "_" + t + " = " + cplex.getValue(l[j][t]));
				System.out.println("r" + j + "_" + t + " = " + cplex.getValue(r[j][t]));
			}
		}
	    
	    cplex.end();
	    
	    return new Solution(_clusters);
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
}
