package branchandprice;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import general.Instance;
import general.Point;
import general.Cluster;

// Class for creating new branches in the Branch-and-Price tree
public final class BranchOnPointCoverings extends AbstractBranchCreator<InputData, PotentialCluster, ClusteringPricingProblem>
{
	// Instance
	private Instance _instance;
	
	// Branching candidate
	private int _pointIndex;
	private int _dimension;
	private double _threshold;
	private boolean _max;

	// Tolerance for considering two cluster sides different
	private static double _tolerance = 0.01;

	// Constructor
    public BranchOnPointCoverings(InputData dataModel, ClusteringPricingProblem pricingProblem)
    {
        super(dataModel, pricingProblem);
        _instance = dataModel.getInstance();
    }

    // Determine on which point, dimension, threshold, and cluster side we are going to branch.
    @Override
    protected boolean canPerformBranching(List<PotentialCluster> solution)
    {
//    	System.out.println("B Branching!");

    	// TODO: Se puede mejorar la complejidad?
    	for(int i=0; i<_instance.getPoints(); ++i)
    	{
    		Point point = _instance.getPoint(i);
    		List<Cluster> including = solution.stream().filter(c -> !c.isArtificialColumn).map(c -> c.getCluster()).filter(c -> c.contains(point)).collect((Collectors.toList()));
    		
//        	System.out.println("B Point: " + point);
//        	for(Cluster c: including)
//            	System.out.println("B Including cluster: " + c);
        	
    		// TODO: Se pueden tener en cuenta todos los clusters al mismo tiempo?
    		if( including.size() >= 2 )
    		{
    			Cluster primero = including.get(0);
    			Cluster segundo = including.get(1);
    			
//    			System.out.println("B primero: " + primero + " " + primero.min(0) + " " + primero.max(0) + " " + primero.min(1) + " " + primero.max(1));
//    			System.out.println("B segundo: " + segundo + " " + segundo.min(0) + " " + segundo.max(0) + " " + segundo.min(1) + " " + segundo.max(1));
    			
    			for(int t=0; t<_instance.getDimension(); ++t)
    			{
    				if( Math.abs(primero.max(t) - segundo.max(t)) > _tolerance )
	    			{
	    				_pointIndex = i;
	    				_dimension = t;
	    				_threshold = (primero.max(t) + segundo.max(t)) / 2;
	    				_max = true;
	    				
//	    	        	System.out.println("B branch: " + i + " " + t + " " + _threshold + " " + _max);
	    				return true;
	    			}

    				if( Math.abs(primero.min(t) - segundo.min(t)) > _tolerance )
	    			{
	    				_pointIndex = i;
	    				_dimension = t;
	    				_threshold = (primero.min(t) + segundo.min(t)) / 2;
	    				_max = false;
	    				
//	    	        	System.out.println("B branch: " + i + " " + t + " " + _threshold + " " + _max);
	    				return true;
	    			}
    			}
    		}
    	}
    		
 		return false;
    }

    // Create the branches
    @Override
    protected List<BAPNode<InputData, PotentialCluster>> getBranches(BAPNode<InputData, PotentialCluster> parentNode)
    {
        // Branch 1: cluster side >= threshold
        ConstraintOnClusterSide branchingDecision1 = new ConstraintOnClusterSide(_instance.getPoint(_pointIndex), _pointIndex, _dimension, _threshold, _max, true);
        BAPNode<InputData,PotentialCluster> node2 = this.createBranch(parentNode, branchingDecision1, parentNode.getSolution(), parentNode.getInequalities());

        // Branch 2: cluster side <= threshold
        ConstraintOnClusterSide branchingDecision2 = new ConstraintOnClusterSide(_instance.getPoint(_pointIndex), _pointIndex, _dimension, _threshold, _max, false);
        BAPNode<InputData,PotentialCluster> node1 = this.createBranch(parentNode, branchingDecision2, parentNode.getSolution(), parentNode.getInequalities());

        return Arrays.asList(node1, node2);
    }
}