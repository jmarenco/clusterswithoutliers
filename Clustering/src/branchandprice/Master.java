package branchandprice;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import general.Instance;
import general.Cluster;

// Defines the master problem
public final class Master extends AbstractMaster<InputData, PotentialCluster, ClusteringPricingProblem, ClusteringMasterData>
{
	private Instance _instance;
    private IloObjective _obj; // Objective function
    private IloRange[] _binding; // Constraints
    private IloRange _numberOfClusters;
    private IloRange _numberOfOutliers;
    private IloRange[] _allConstraints;
    private IloNumVar[] _y;

    public Master(InputData dataModel, ClusteringPricingProblem pricingProblem)
    {
        super(dataModel, pricingProblem, OptimizationSense.MINIMIZE);
    	_instance = dataModel.getInstance();

    	System.out.println("Master constructor. Columns: " + masterData.getNrColumns());
    }

    // Builds the master model
    @Override
    protected ClusteringMasterData buildModel()
    {
    	// This method is called from super, so _instance is not initialized yet
    	_instance = dataModel.getInstance();

    	IloCplex cplex=null;

    	try
        {
            cplex=new IloCplex(); // Create cplex instance
            cplex.setOut(null); // Disable cplex output
            cplex.setParam(IloCplex.IntParam.Threads,config.MAXTHREADS); // Set number of threads that may be used by the master

            // Define objective
            _obj=cplex.addMinimize();

            // Create y variables
            _y = new IloNumVar[_instance.getPoints()];
            for(int i=0; i<_instance.getPoints(); i++)
            	_y[i] = cplex.numVar(0, 1, "y" + i);

            // Define binding constraints
            _binding = new IloRange[_instance.getPoints()];
            for(int i=0; i<_instance.getPoints(); i++)
            {
            	IloNumExpr lhs = cplex.linearIntExpr();
            	lhs = cplex.sum(lhs, cplex.prod(-1, _y[i]));
                _binding[i] = cplex.addGe(lhs, 0, "bind" + i);
            }
            
            // Define constraint on the number of clusters
            _numberOfClusters = cplex.addGe(cplex.linearIntExpr(), -_instance.getClusters(), "clus");
            
            // Define constraint on the number of outliers
            IloNumExpr lhs = cplex.linearIntExpr();
            for(int i=0; i<_instance.getPoints(); i++)
            	lhs = cplex.sum(lhs, _y[i]);
            
            _numberOfOutliers = cplex.addGe(lhs, _instance.getPoints() - _instance.getOutliers(), "out");
            
            // Collects all constraints into a single array
            _allConstraints = new IloRange[_instance.getPoints() + 2];

            for(int i=0; i<_instance.getPoints(); i++)
            	_allConstraints[i] = _binding[i];
            
            _allConstraints[_instance.getPoints()] = _numberOfClusters;
            _allConstraints[_instance.getPoints() + 1] = _numberOfOutliers;
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }

        Map<ClusteringPricingProblem, OrderedBiMap<PotentialCluster, IloNumVar>> varMap = new LinkedHashMap<>();
        ClusteringPricingProblem pricingProblem = this.pricingProblems.get(0);
        varMap.put(pricingProblem, new OrderedBiMap<>());

        // Create a new data object which will store information from the master
        return new ClusteringMasterData(cplex, varMap);
    }

    // Solve the master problem
    @Override
    protected boolean solveMasterProblem(long timeLimit) throws TimeLimitExceededException
    {
        try
        {
            // Set time limit
            double timeRemaining=Math.max(1,(timeLimit-System.currentTimeMillis())/1000.0);
            masterData.cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); // set time limit in seconds

            // Potentially export the model
            // if(config.EXPORT_MODEL)
            // 	masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + "master_" + this.getIterationCount() + ".lp");
            
//            System.out.println("*** Exporting master model ...");
//            masterData.cplex.exportModel("master_" + this.getIterationCount() + ".lp");

            // Solve the model
            if( !masterData.cplex.solve() || masterData.cplex.getStatus() != IloCplex.Status.Optimal )
            {
                if( masterData.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) //Aborted due to time limit
                    throw new TimeLimitExceededException();
                else
                    throw new RuntimeException("Master problem solve failed! Status: " + masterData.cplex.getStatus());
            }
            else
            {
                masterData.objectiveValue = masterData.cplex.getObjValue();
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return true;
    }

    // Extracts information from the master problem required by the pricing problems
    @Override
    public void initializePricingProblem(ClusteringPricingProblem pricingProblem)
    {
        try
        {
            double[] dualValues=masterData.cplex.getDuals(_allConstraints);
            pricingProblem.initPricingProblem(dualValues);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Adds a new column to the master problem
    @Override
    public void addColumn(PotentialCluster column)
    {
        try
        {
        	Cluster cluster = column.getCluster();
        	
            // Register column with objective
        	System.out.println("Registering column - cost: " + column.getCost() + " - " + column.getCluster());
            IloColumn iloColumn = masterData.cplex.column(_obj, column.getCost());

            // Register column with the constraints
            for(int i=0; i<_instance.getPoints(); ++i) if( cluster.contains(_instance.getPoint(i)) )
                iloColumn = iloColumn.and(masterData.cplex.column(_binding[i], 1));
            
            iloColumn = iloColumn.and(masterData.cplex.column(_numberOfClusters, -1));

            // Create the variable and store it
            IloNumVar var = masterData.cplex.numVar(iloColumn, 0, Double.MAX_VALUE, "x" + masterData.getNrColumns());
            masterData.cplex.add(var);
            masterData.addColumn(column,var);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Gets the solution from the master problem. Returns all non-zero valued columns from the master problem
    @Override
    public List<PotentialCluster> getSolution()
    {
        List<PotentialCluster> solution=new ArrayList<>();
        try
        {
            PotentialCluster[] clusters = masterData.getColumnsForPricingProblemAsList().toArray(new PotentialCluster[masterData.getNrColumns()]);
            IloNumVar[] vars = masterData.getVarMap().getValuesAsArray(new IloNumVar[masterData.getNrColumns()]);
            double[] values = masterData.cplex.getValues(vars);

            System.out.println("Retrieving master solution");
            for(int i=0; i<clusters.length; i++)
            {
                System.out.println("  Cluster " + i + ", val = " + values[i]);
                clusters[i].value = values[i];
                if( values[i] >= config.PRECISION )
                    solution.add(clusters[i]);
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        System.out.println("Returning solution with " + solution.size() + " clusters");
        return solution;
    }

    // Prints the solution
    @Override
    public void printSolution()
    {
        List<PotentialCluster> solution = this.getSolution();
        for(PotentialCluster potentialCluster: solution)
            System.out.println(potentialCluster.getCluster());
    }

    // Closes the master problem
    @Override
    public void close()
    {
        masterData.cplex.end();
    }

    // Listen to branching decisions
    @Override
    public void branchingDecisionPerformed(BranchingDecision bd)
    {
        //For simplicity, we simply destroy the master problem and rebuild it. Of course, something more sophisticated may be used which retains the master problem.
        this.close(); //Close the old cplex model
        masterData = this.buildModel(); //Create a new model without any columns
    }

    // Undo branching decisions during backtracking in the Branch-and-Price tree
    @Override
    public void branchingDecisionReversed(BranchingDecision bd)
    {
        //No action required
    }
}