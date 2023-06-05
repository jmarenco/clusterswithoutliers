package branchandprice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import general.Cluster;
import general.Instance;
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class PricingZLRModel implements Pricing
{
	// Input data
	private Master _master;
	private Instance _instance;
	private int p; // Points
	private int d; // Dimension

	// Solver
    private IloCplex cplex;
    
    // Objective function
    private IloObjective obj;

    // Branching constraints
    private Map<BranchingDecision, IloConstraint> branchingConstraints; //Constraints added to enforce branching decisions

    // Variables
	private IloNumVar[] z;
	private IloNumVar[] r;
	private IloNumVar[] l;
	
	// Parameters
	private static double _reducedCostThreshold = -0.0001; // Threshold for considering the objective as negative
	private static double _variableThreshold = 0.05; // Threshold for considering a variable as null
	private static boolean _stopWhenNegative = false;

	// Statistics
	private double _solvingTime = 0;
	
	// Creates a new solver instance for a particular pricing problem
    public PricingZLRModel(Master master)
    {
    	_master = master;
        _instance = master.getInstance();
		p = _instance.getPoints();
		d = _instance.getDimension();
        
        this.buildModel();
    }

    // Build the MIP model
    private void buildModel()
    {
        try
        {
        	createSolver();
    		createVariables();
    		createConstraints();
    		createObjective();

            branchingConstraints = new HashMap<BranchingDecision, IloConstraint>();
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    private void createSolver() throws IloException
    {
        cplex = new IloCplex();
        cplex.setParam(IloCplex.IntParam.AdvInd, 0);
        cplex.setParam(IloCplex.IntParam.Threads, 1);
        cplex.setOut(null);
        
    	// The parameters cplex.uppercutoff = 0 and MIP integer solution limit = 1
    	// make Cplex stop as soon as it finds a solution with objective function < 0,
    	// but may degrade the performance since heuristics are not employed from nodes with
    	// relaxation objective >= 0, which are pruned
    	
        if( _stopWhenNegative == true )
        {
        	cplex.setParam(IloCplex.Param.MIP.Tolerances.UpperCutoff, _reducedCostThreshold);
        	cplex.setParam(IloCplex.Param.MIP.Limits.Solutions, 1);
        }
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
			IloNumExpr lhs3 = cplex.linearIntExpr();
			IloNumExpr lhs4 = cplex.linearIntExpr();
			
			lhs1 = cplex.sum(lhs1, l[t]);
			lhs1 = cplex.sum(lhs1, cplex.prod(_instance.max(t) -_instance.getPoint(i).get(t), z[i]));
			
			lhs2 = cplex.sum(lhs2, r[t]);
			lhs2 = cplex.sum(lhs2, cplex.prod(_instance.min(t) -_instance.getPoint(i).get(t), z[i]));

			lhs3 = cplex.sum(lhs3, l[t]);
			lhs3 = cplex.sum(lhs3, cplex.prod(_instance.min(t) -_instance.getPoint(i).get(t), z[i]));
			
			lhs4 = cplex.sum(lhs4, r[t]);
			lhs4 = cplex.sum(lhs4, cplex.prod(_instance.max(t) -_instance.getPoint(i).get(t), z[i]));

			for(int j=0; j<p; ++j) if( _instance.getPoint(j).get(t) < _instance.getPoint(i).get(t) )
				lhs3 = cplex.sum(lhs3, cplex.prod(-_instance.min(t) +_instance.getPoint(i).get(t), z[j]));

			for(int j=0; j<p; ++j) if( _instance.getPoint(j).get(t) > _instance.getPoint(i).get(t) )
				lhs4 = cplex.sum(lhs4, cplex.prod(-_instance.max(t) +_instance.getPoint(i).get(t), z[j]));
				
			cplex.addLe(lhs1, _instance.max(t), "l" + i + "_" + t);
		    cplex.addGe(lhs2, _instance.min(t), "r" + i + "_" + t);
			cplex.addGe(lhs3, _instance.min(t), "ls" + i + "_" + t);
		    cplex.addLe(lhs4, _instance.max(t), "rs" + i + "_" + t);
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
		obj = cplex.addMinimize();
	}
	
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit)
    {
        List<Cluster> newPatterns = new ArrayList<>();
        try
        {
            cplex.setParam(IloCplex.DoubleParam.TiLim, timeLimit); //set time limit in seconds
//            cplex.exportModel("/home/jmarenco/Desktop/pricing.lp");

       		// Solve the problem and check the solution status
            double start = System.currentTimeMillis();
            boolean solved = cplex.solve();
            
            _solvingTime += (System.currentTimeMillis() - start) / 1000.0;

       		if( cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) // Aborted due to time limit
       			return newPatterns;

            if( _stopWhenNegative == false )
            {
           		if( cplex.getStatus() == IloCplex.Status.Infeasible ) // Pricing problem infeasible
           			throw new RuntimeException("Pricing problem infeasible");
           		
            	if( solved == false || cplex.getStatus() != IloCplex.Status.Optimal )
           			throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus() + ", obj: " + cplex.getObjValue());
            }
            else
            {
           		if( cplex.getStatus() != IloCplex.Status.Infeasible && cplex.getObjValue() > -_reducedCostThreshold )
           			throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus() + ", obj: " + cplex.getObjValue());
            }
            
//            System.out.println("Pricing problem solved - Obj = " + cplex.getObjValue());

            // Generate new column if it has negative reduced cost
            if( cplex.getStatus() != IloCplex.Status.Infeasible && cplex.getObjValue() <= _reducedCostThreshold )
            { 
                Cluster cluster = new Cluster();
                double[] values = cplex.getValues(z);

                for(int i=0; i<_instance.getPoints(); ++i)
                {
                  	if( Math.abs(values[i] - 1) < _variableThreshold )
                   		cluster.add(_instance.getPoint(i));
                }
                    	
                newPatterns.add(cluster);
//              System.out.print(" -> " + cluster);
            }
                
//          System.out.println();
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
//        if( newPatterns.get(0).contains(_instance.getPoint(6)) && newPatterns.get(0).contains(_instance.getPoint(3)) && newPatterns.get(0).size() == 2)
//        	System.exit(1);
        
        return newPatterns;
    }

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective()
    {
        try
        {
            double[] dualCosts = _master.getDuals();
    		IloNumExpr fobj = cplex.linearNumExpr();

    		for(int t=0; t<d; ++t)
    		{
    			fobj = cplex.sum(fobj, cplex.prod(1.0, r[t]));
    			fobj = cplex.sum(fobj, cplex.prod(-1.0, l[t]));
    		}
    		
    		for(int i=0; i<p; ++i)
    			fobj = cplex.sum(fobj, cplex.prod(-dualCosts[i], z[i]));
    		
    		fobj = cplex.sum(fobj, dualCosts[p]);
            obj.setExpr(fobj);
            
//            System.out.println("Pricing obj set: " + obj);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Close the pricing problem
    public void close()
    {
        cplex.end();
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc)
    {
    	if( sc instanceof BranchOnSide )
    		performBranchingOnSide((BranchOnSide)sc);
    	
    	if (sc instanceof BranchOnOutlier )
    		performBranchingOnOutlier((BranchOnOutlier)sc);
    }
    
    private void performBranchingOnSide(BranchOnSide sc)
    {
        try
        {
//        	System.out.println("Pricing: Perform branching " + sc);
        	
           	IloNumExpr lhs = cplex.linearIntExpr();
            IloConstraint branchingConstraint = null;

         	if( sc.appliesToMaxSide() )
           		lhs = cplex.sum(lhs, r[sc.getDimension()]);
           	else
           		lhs = cplex.sum(lhs, l[sc.getDimension()]);
            	
           	if( sc.isLowerBound() )
           	{
           		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.min(sc.getDimension()), z[sc.getPoint()]));
           		branchingConstraint = cplex.addGe(lhs, _instance.min(sc.getDimension()));
           	}
           	else
           	{
           		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.max(sc.getDimension()), z[sc.getPoint()]));
           		branchingConstraint = cplex.addLe(lhs, _instance.max(sc.getDimension()));
           	}
                
            branchingConstraints.put(sc, branchingConstraint);

//            System.out.println(">>> Branching constraint added: ");
//            System.out.println("    " + sc);
//            System.out.println("    " + branchingConstraint);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    private void performBranchingOnOutlier(BranchOnOutlier sc)
    {
        try
        {
//        	System.out.println("Pricing: Perform branching " + sc);
        	
           	IloNumExpr lhs = cplex.linearIntExpr();
           	lhs = cplex.sum(lhs, z[sc.getPoint()]);

           	IloConstraint branchingConstraint = sc.mustBeOutlier() ? cplex.addEq(lhs, 0) : cplex.addLe(lhs, 1);
            branchingConstraints.put(sc, branchingConstraint);

//            System.out.println(">>> Branching constraint added: ");
//            System.out.println("    " + sc);
//            System.out.println("    " + branchingConstraint);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc)
    {
        try
        {
//        	System.out.println("Pricing: Reverse branching " + sc);
            cplex.remove(branchingConstraints.get(sc));

//            System.out.println(">>> Branching decision reversed: ");
//            System.out.println("    " + sc);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    public static void stopWhenNegative(boolean value)
    {
    	_stopWhenNegative = value;
    }
    
    public static boolean stopWhenNegative()
    {
    	return _stopWhenNegative;
    }
    
    public double getSolvingTime()
    {
    	return _solvingTime;
    }
}
