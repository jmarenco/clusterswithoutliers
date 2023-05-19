package branchandprice;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchAndPrice;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import general.Cluster;
import general.Point;
import general.Instance;

import java.util.*;

// Branch-and-Price implementation
public final class BranchAndPrice extends AbstractBranchAndPrice<InputData, PotentialCluster, ClusteringPricingProblem>
{
	private Instance _instance;
	
    public BranchAndPrice(InputData dataModel,
                          AbstractMaster<InputData, PotentialCluster, ClusteringPricingProblem, ? extends MasterData> master,
                          ClusteringPricingProblem pricingProblem,
                          List<Class<? extends AbstractPricingProblemSolver<InputData, PotentialCluster, ClusteringPricingProblem>>> solvers,
                          List<? extends AbstractBranchCreator<InputData, PotentialCluster, ClusteringPricingProblem>> abstractBranchCreators,
                          double lowerBoundOnObjective,
                          double upperBoundOnObjective)
    {
        super(dataModel, master, pricingProblem, solvers, abstractBranchCreators, lowerBoundOnObjective, upperBoundOnObjective);
        _instance = dataModel.getInstance();
    }

    // Generates an artificial solution. Columns in the artificial solution are of high cost such that they never end up in the final solution
    // if a feasible solution exists, since any feasible solution is assumed to be cheaper than the artificial solution. The artificial solution is used
    // to guarantee that the master problem has a feasible solution.
    @Override
    protected List<PotentialCluster> generateInitialFeasibleSolution(BAPNode<InputData, PotentialCluster> node)
    {
        List<PotentialCluster> artificialSolution = new ArrayList<PotentialCluster>();
        artificialSolution.add(new PotentialCluster(pricingProblems.get(0), true, "Artificial", Cluster.withAllPoints(_instance)));
        return artificialSolution;
    }

    /**
     * Checks whether the given node is integer. A solution is integer if every vertex is contained in exactly 1 independent set,
     * that is, if every vertex is assigned a single color.
     * @param node Node in the Branch-and-Price tree
     * @return true if the solution is an integer solution
     */
    @Override
    protected boolean isIntegerNode(BAPNode<InputData, PotentialCluster> node)
    {
    	// TODO: Esta bien este chequeo?
    	Set<Point> puntos = new HashSet<Point>();

    	for(PotentialCluster column: node.getSolution())
        	puntos.addAll(column.getCluster().getPoints());
        
    	return puntos.size() >= _instance.getPoints() - _instance.getOutliers();
    }
}