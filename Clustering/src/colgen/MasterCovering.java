package colgen;

import java.util.ArrayList;

import general.Cluster;
import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

public class MasterCovering
{
	private Instance _instance;
	private ArrayList<Cluster> _clusters;
	private double _obj;
	private double[] _primal;
	private double[] _dual;
	
	private boolean _verbose = false;
	
	public MasterCovering(Instance instance, ArrayList<Cluster> clusters)
	{
		_instance = instance;
		_clusters = clusters;
	}
	
	public boolean solve(boolean integer) throws IloException
	{
		// Model sizes
		int p = _instance.getPoints();
		int m = p + 2;
		int n = _clusters.size();

		// Create solver
		IloCplex cplex = new IloCplex();

		if( _verbose == false )
		{
			cplex.setOut(null);
			cplex.setWarning(null);
		}

		// Create variables
		IloNumVar[] x = new IloNumVar[n];
		IloNumVar[] y = new IloNumVar[p];
		
	    for(int j=0; j<n; ++j)
	    	x[j] = integer ? cplex.boolVar("x" + j) : cplex.numVar(0, 1, "x" + j);

	    for(int i=0; i<p; ++i)
	    	y[i] = integer ? cplex.boolVar("y" + i) : cplex.numVar(0, 1, "y" + i);

	    // Create constraints
	    IloRange[] constraints = new IloRange[m];
	    
	    for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
		    for(int j=0; j<n; ++j) if( _clusters.get(j).contains(_instance.getPoint(i)) )
		    	lhs = cplex.sum(lhs, x[j]);

		    lhs = cplex.sum(lhs, cplex.prod(-1, y[i]));
		    constraints[i] = cplex.addGe(lhs, 0, "c" + i);
	    }
	    
		IloNumExpr lhs1 = cplex.linearIntExpr();
		IloNumExpr lhs2 = cplex.linearIntExpr();
		
	    for(int j=0; j<n; ++j)
	    	lhs1 = cplex.sum(lhs1, cplex.prod(-1, x[j]));
	    
	    for(int i=0; i<p; ++i)
	    	lhs2 = cplex.sum(lhs2, y[i]);
	    
	    constraints[p] = cplex.addGe(lhs1, -_instance.getClusters(), "clus");
	    constraints[p+1] = cplex.addGe(lhs2, _instance.getPoints() - _instance.getOutliers(), "out");
	    
	    // Create objective
		IloNumExpr fobj = cplex.linearNumExpr();
	    for(int j=0; j<n; ++j)
			fobj = cplex.sum(fobj, cplex.prod(_clusters.get(j).totalDistanceToCentroid(), x[j]));
		
		cplex.addMinimize(fobj);
		
		// Solve model
		if( integer == false )
			cplex.setOut(null);
		
//		if( integer == true )
//			cplex.exportModel("master-int.lp");
//		else
//			cplex.exportModel("master-rel.lp");
		
		cplex.solve();

		boolean ret = cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible;
	    if( ret == true )
		{
			_obj = cplex.getObjValue();
			_primal = cplex.getValues(x);
			_dual = integer ? null : cplex.getDuals(constraints);

//			if( integer == false)
//			{
//				System.out.println("----------------------------");
//				System.out.println("rel:");
//				
//			    for(int j=0; j<n; ++j)
//					System.out.println("x" + j + " = " + cplex.getValue(x[j]));
//
//			    for(int i=0; i<p; ++i)
//					System.out.println("y" + i + " = " + cplex.getValue(y[i]));
//			    
//			    for(int i=0; i<p+2; ++i)
//					System.out.println("constr " + i + " = " + cplex.getDual(constraints[i]));
//	
//				System.out.println("----------------------------");
//			}
		}
	    
	    cplex.end();
	    return ret;
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
	
	public double getClustersDual()
	{
		return _dual[_dual.length-2];
	}

	public double getOutliersDual()
	{
		return _dual[_dual.length-1];
	}
	
	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
	
	public double reducedCost(Cluster cluster)
	{
		double ret = cluster.totalDistanceToCentroid() - this.getClustersDual() - this.getOutliersDual();
		
		for(int i=0; i<_instance.getPoints(); ++i) if( cluster.contains(_instance.getPoint(i)) )
			ret -= this.getDual(i);

		return ret;
	}
}
