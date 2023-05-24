package branchandprice;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import general.Cluster;

import java.util.*;
import general.Instance;;

// Column generation procedure
public final class ClusteringCalculator
{
	private InputData _inputData;
	private Instance _instance;
	
    public ClusteringCalculator(InputData inputData)
    {
    	_inputData = inputData;
    	_instance = inputData.getInstance();
    }
    
    public void solve()
    {
        //Create Pricing problem
        ClusteringPricingProblem pricingProblem = new ClusteringPricingProblem(_inputData, "clusteringPricingProblem");

        //Create the Master Problem
        Master master = new Master(_inputData, pricingProblem);

        //Define which solvers to use for the pricing problem
        List<Class<? extends AbstractPricingProblemSolver<InputData, PotentialCluster, ClusteringPricingProblem>>> solvers = Collections.singletonList(ExactPricingProblemSolver.class);

        //Optional: Get an initial solution
//        List<PotentialCluster> initSolution = this.getInitialSolution(pricingProblem);
//        double upperBound=initSolution.stream().mapToDouble(p -> p.getCluster().totalSpan()).sum();
        double upperBound=10000;

        //Optional: Get a lower bound on the optimum solution, e.g. largest clique in the graph
        double lowerBound=this.calculateLowerBound();

        //Define Branch creators
        List<? extends AbstractBranchCreator<InputData, PotentialCluster, ClusteringPricingProblem>> branchCreators = Collections.singletonList(new BranchOnPointCoverings(_inputData, pricingProblem));

        //Create a Branch-and-Price instance, and provide the initial solution as a warm-start
        BranchAndPrice bap = new BranchAndPrice(_inputData, master, pricingProblem, solvers, branchCreators, lowerBound, upperBound);
//        bap.warmStart((int)Math.ceil(upperBound), initSolution);

        //OPTIONAL: Attach a debugger
        //new SimpleDebugger(bap, true);

        //OPTIONAL: Attach a logger to the Branch-and-Price procedure.
        //new SimpleBAPLogger(bap, new File("./output/coloring.log"));

        //Solve the Graph Coloring problem through Branch-and-Price
        bap.runBranchAndPrice(System.currentTimeMillis() + 8000000L);

        //Print solution:
        System.out.println("================ Solution ================");
        System.out.println("BAP terminated with objective: " + bap.getObjective());
        System.out.println("Total Number of iterations: " + bap.getTotalNrIterations());
        System.out.println("Total Number of processed nodes: " + bap.getNumberOfProcessedNodes());
        System.out.println("Total Time spent on master problems: " + bap.getMasterSolveTime() + " Total time spent on pricing problems: " + bap.getPricingSolveTime());
        
        if (bap.hasSolution())
        {
            System.out.println("Solution is optimal: " + bap.isOptimal());
            System.out.println("Columns (only non-zero columns are returned):");
        
            List<PotentialCluster> solution = bap.getSolution();
            for (PotentialCluster column: solution)
                System.out.println(column);
        }

        //Clean up:
        bap.close(); //Close master and pricing problems
    }

    // Initial solution through a greedy algorithm
    public List<PotentialCluster> getInitialSolution(ClusteringPricingProblem pricingProblem)
    {
    	// TODO: Mejorar!
    	Cluster cluster = Cluster.withAllPoints(_instance);
        
    	List<PotentialCluster> initialSolution = new ArrayList<>();
        initialSolution.add(new PotentialCluster(pricingProblem, false, "initialColumn", cluster));

        return initialSolution;
    }

    // Calculate a lower bound on the optimal value
    private int calculateLowerBound()
    {
    	// TODO: Calcular?
        return 0;
    }
}