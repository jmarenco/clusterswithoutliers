package branchandprice;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import general.Cluster;
import general.Instance;

// Algorithm implementation which solves the pricing problem to optimality.
// This solver is based on an exact MIP implementation using Cplex.
public final class ExactPricingProblemSolver extends AbstractPricingProblemSolver<InputData, PotentialCluster, ClusteringPricingProblem>
{
	// Input data
	private Instance _instance;
	private int p; // Points
	private int d; // Dimension

	// Solver
    private IloCplex cplex;
    
    // Objective function
    private IloObjective obj;

    // Branching constraints
    private Map<ConstraintOnClusterSide, IloConstraint> branchingConstraints; //Constraints added to enforce branching decisions

    // Variables
	private IloNumVar[] z;
	private IloNumVar[] r;
	private IloNumVar[] l;

	// Creates a new solver instance for a particular pricing problem
    public ExactPricingProblemSolver(InputData dataModel, ClusteringPricingProblem pricingProblem)
    {
        super(dataModel, pricingProblem);

        _instance = dataModel.getInstance();
		p = _instance.getPoints();
		d = _instance.getDimension();
        
        this.name = "ExactClusterFinder";
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

            branchingConstraints = new HashMap<ConstraintOnClusterSide, IloConstraint>();
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
		obj = cplex.addMinimize();
	}
	
	// Main method for solving the pricing problem
    @Override
    protected List<PotentialCluster> generateNewColumns() throws TimeLimitExceededException
    {
        List<PotentialCluster> newPatterns = new ArrayList<>();
        try
        {
            // Compute how much time we may take to solve the pricing problem
            double timeRemaining = Math.max(1, (timeLimit-System.currentTimeMillis()) / 1000.0);
            cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); //set time limit in seconds

//            System.out.println("*** Exporting pricing model ...");
//            cplex.exportModel("pricing.lp");

            // Solve the problem and check the solution nodeStatus
            if( !cplex.solve() || cplex.getStatus() != IloCplex.Status.Optimal )
            {
                if( cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) // Aborted due to time limit
                {
                    throw new TimeLimitExceededException();
                }
                else if( cplex.getStatus() == IloCplex.Status.Infeasible ) // Pricing problem infeasible
                {
                    pricingProblemInfeasible = true;
                    this.objective = Double.MAX_VALUE;
                    throw new RuntimeException("Pricing problem infeasible");
                }
                else
                {
                    throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus());
                }
            }
            else // Pricing problem solved to optimality
            {
                this.pricingProblemInfeasible = false;
                this.objective = cplex.getObjValue();
                System.out.println("Pricing obj: " + objective);

                // Generate new column if it has negative reduced cost
                if( objective <= -config.PRECISION )
                { 
                    double[] values = cplex.getValues(z);
                    Set<Integer> pointIndices = IntStream.range(0, _instance.getPoints()).filter(i -> MathProgrammingUtil.doubleToBoolean(values[i])).boxed().collect(Collectors.toSet());
                    PotentialCluster column = new PotentialCluster(pricingProblem, false, this.getName(), Cluster.fromSet(_instance, pointIndices));
                    newPatterns.add(column);
                }
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return newPatterns;
    }

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    @Override
    protected void setObjective()
    {
        try
        {
            double[] dualCosts = pricingProblem.dualCosts;
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
            
            System.out.println("Pricing obj set: " + obj);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Close the pricing problem
    @Override
    public void close()
    {
        cplex.end();
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    @Override
    public void branchingDecisionPerformed(BranchingDecision bd)
    {
        try
        {
            if( bd instanceof ConstraintOnClusterSide )
            {
            	ConstraintOnClusterSide sc = (ConstraintOnClusterSide) bd;
            	IloNumExpr lhs = cplex.linearIntExpr();
                IloConstraint branchingConstraint = null;

            	if( sc.appliesToMaxSide() )
            		lhs = cplex.sum(lhs, r[sc.getDimension()]);
            	else
            		lhs = cplex.sum(lhs, l[sc.getDimension()]);
            	
            	if( sc.isLowerBound() )
            	{
            		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.min(sc.getDimension()), z[sc.getPoint()]));
            		branchingConstraint = cplex.addGe(_instance.min(sc.getDimension()), lhs);
            	}
            	else
            	{
            		lhs = cplex.sum(lhs, cplex.prod(-sc.getThreshold() + _instance.max(sc.getDimension()), z[sc.getPoint()]));
            		branchingConstraint = cplex.addLe(_instance.max(sc.getDimension()), lhs);
            	}
                
                branchingConstraints.put(sc, branchingConstraint);
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    @Override
    public void branchingDecisionReversed(BranchingDecision bd)
    {
        try
        {
            if( bd instanceof ConstraintOnClusterSide )
            {
            	ConstraintOnClusterSide sc = (ConstraintOnClusterSide) bd;
                cplex.remove(branchingConstraints.get(sc));
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
}