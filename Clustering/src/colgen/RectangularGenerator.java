package colgen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import general.Cluster;
import general.Instance;
import general.RectangularCluster;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;

public class RectangularGenerator
{
	// Instance and master
	private Instance _instance;
	private MasterCovering _master;
	
	// Model sizes
	private int p; // Points
	private int d; // Dimension

	// Solver
	private IloCplex cplex;

    // Variables
	private IloNumVar[] z;
	private IloNumVar[] r;
	private IloNumVar[] l;
	
	public RectangularGenerator(Instance instance, MasterCovering master)
	{
		_instance = instance;
		_master = master;

		p = _instance.getPoints();
		d = _instance.getDimension();
	}
	
	public List<Cluster> solve() throws IloException
	{
		createSolver();
		createVariables();
		createConstraints();
		createObjective();
		
		solveModel();
		List<Cluster> ret = obtainSolution();
	    closeSolver();
	    
	    return ret;
	}

	private void createSolver() throws IloException
	{
		cplex = new IloCplex();

		cplex.setOut(null);
		cplex.setWarning(null);
	}

	private void createVariables() throws IloException
	{
		z = new IloNumVar[p];
		r = new IloNumVar[d];
		l = new IloNumVar[d];
		
		for(int i=0; i<p; ++i)
	    	z[i] = cplex.boolVar("z" + i);

		for(int t=0; t<d; ++t)
	    	r[t] = cplex.numVar(_instance.min(t), _instance.max(t), "r" + t);

		for(int t=0; t<d; ++t)
	    	l[t] = cplex.numVar(_instance.min(t), _instance.max(t), "l" + t);
	}

	private void createConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
		for(int t=0; t<d; ++t)
	    {
			IloNumExpr lhs1 = cplex.linearIntExpr();
			IloNumExpr lhs2 = cplex.linearIntExpr();
			
			lhs1 = cplex.sum(lhs1, l[t]);
			lhs1 = cplex.sum(lhs1, cplex.prod(_instance.max(t) -_instance.getPoint(i).get(t), z[i]));
			
			lhs2 = cplex.sum(lhs2, r[t]);
			lhs2 = cplex.sum(lhs2, cplex.prod(_instance.min(t) -_instance.getPoint(i).get(t), z[i]));

		    cplex.addLe(lhs1, _instance.max(t), "l" + i + "_" + t);
		    cplex.addGe(lhs2, _instance.min(t), "r" + i + "_" + t);
	    }
		
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			
			lhs = cplex.sum(lhs, cplex.prod(1.0, l[t]));
			lhs = cplex.sum(lhs, cplex.prod(-1.0, r[t]));
			
		    cplex.addLe(lhs, 0, "rel" + t);
		}
	}
	
	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int t=0; t<d; ++t)
		{
			fobj = cplex.sum(fobj, cplex.prod(1.0, r[t]));
			fobj = cplex.sum(fobj, cplex.prod(-1.0, l[t]));
		}
		
		for(int i=0; i<p; ++i)
			fobj = cplex.sum(fobj, cplex.prod(-_master.getDual(i), z[i]));
		
		fobj = cplex.sum(fobj, _master.getClustersDual());
		
		cplex.addMinimize(fobj);
	}
	
	private void solveModel() throws IloException
	{
//		cplex.exportModel("model.lp");
		
//		System.out.println(_master.getClustersDual());
//		System.out.println(_master.getOutliersDual());
//		
//		for(int i=0; i<p; ++i)
//			System.out.println(i + " = " + _master.getDual(i));
		
		cplex.setOut(null);
		cplex.solve();
	}

	private List<Cluster> obtainSolution() throws IloException, UnknownObjectException
	{
		ArrayList<Cluster> list = new ArrayList<Cluster>();
		
    	if( (cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getObjValue() < -0.01 )
		{
    		int n = cplex.getSolnPoolNsolns();
    		for (int j = 0; j < n; j++) if (cplex.getObjValue(j) < -0.01)
    		{
	    		RectangularCluster ret = new RectangularCluster(d);
		    		
	//    		System.out.println("------------------------------");
	//    		System.out.println("Gen sol:");
	    		
				for(int t=0; t<d; ++t)
				{
					ret.setMin(t, cplex.getValue(l[t], j));
					ret.setMax(t, cplex.getValue(r[t], j));
					
	//				System.out.println("l" + t + " = " + cplex.getValue(l[t]));
	//				System.out.println("r" + t + " = " + cplex.getValue(r[t]));
				}
				
				for(int i=0; i<p; ++i) if( cplex.getValue(z[i], j) > 0.9 )
				{
					ret.add(_instance.getPoint(i));
	//				System.out.println("z" + i + " = " + cplex.getValue(z[i]));
				}
				
				list.add(ret);
    		}
    		
//    		System.out.println("------------------------------");
		}
    	
    	return list;
	}
	
	private void closeSolver()
	{
		cplex.end();
	}
}
