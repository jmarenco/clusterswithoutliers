package clustering;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

public class Master
{
	private Instance _instance;
	private ArrayList<Cluster> _clusters;
	private double _obj;
	private double[] _primal;
	private double[] _dual;
	
	public Master(Instance instance, ArrayList<Cluster> clusters)
	{
		_instance = instance;
		_clusters = clusters;
	}
	
	public boolean solve() throws IloException
	{
		// Model sizes
		int p = _instance.getPoints();
		int m = p + 2;
		int n = _clusters.size();

		// Create solver
		IloCplex cplex = new IloCplex();

	    // Create variables
		IloNumVar[] variables = new IloNumVar[n];
	    for(int j=0; j<n; ++j)
	    	variables[j] = cplex.numVar(0, 1, "x" + j);

	    // Create constraints
	    IloRange[] constraints = new IloRange[m];
	    
	    for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
		    for(int j=0; j<n; ++j) if( _clusters.get(j).contains(_instance.getPoint(i)) )
		    	lhs = cplex.sum(lhs, variables[j]);
		    
		    constraints[i] = cplex.addLe(lhs, 1, "c" + i);
	    }
	    
		IloNumExpr lhs1 = cplex.linearIntExpr();
		IloNumExpr lhs2 = cplex.linearIntExpr();

	    for(int j=0; j<n; ++j)
	    {
	    	lhs1 = cplex.sum(lhs1, variables[j]);
	    	lhs2 = cplex.sum(lhs2, cplex.prod(_clusters.get(j).size(), variables[j]));
	    }
	    
	    constraints[p] = cplex.addLe(lhs1, _instance.getClusters(), "clus");
	    constraints[p+1] = cplex.addGe(lhs2, _instance.getPoints() - _instance.getOutliers(), "out");
	    
	    // Create objective
		IloNumExpr fobj = cplex.linearIntExpr();
	    for(int j=0; j<n; ++j)
			fobj = cplex.sum(fobj, cplex.prod(_clusters.get(j).objective(), variables[j]));
		
		cplex.addMinimize(fobj);
		
		// Solve model
		cplex.solve();
		cplex.exportModel("modelo.lp");

		System.out.println();
		System.out.println(cplex.getStatus());
		
	    for(int j=0; j<n; ++j)
	    	System.out.println(variables[j] + " = " + cplex.getValue(variables[j]) + ", RC = " + cplex.getReducedCost(variables[j]));
	    
	    System.out.println();

	    if( cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible)
		{
			_obj = cplex.getObjValue();
			_primal = cplex.getValues(variables);
			_dual = cplex.getDuals(constraints);
		}
		
		return cplex.getStatus() == Status.Optimal;
	}
	
	public double getObjective()
	{
		return _obj;
	}
	
	public double getPrimal(int i)
	{
		return _primal[i];
	}

	public double getDual(int i)
	{
		return _dual[i];
	}
}
