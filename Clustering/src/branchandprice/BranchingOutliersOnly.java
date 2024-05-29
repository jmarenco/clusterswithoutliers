package branchandprice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import general.Cluster;
import general.Instance;

public class BranchingOutliersOnly extends Branching
{
	// Tolerance for considering two cluster sides different
	private static double _tolerance = 0.01;

	// Constructor
    public BranchingOutliersOnly(Instance instance)
    {
    	super(instance);
    }

    // Create the branches
    public List<BranchingDecision> getBranches(Map<Cluster, Double> solution, double[] outlier)
    {
    	// This branching is incomplete! It has been added in order to solve the
    	// problem relaxation given by just this branching rule (i.e., by relaxing the
    	// other variables to take fractional values)
    	
    	return branchOnOutliers(outlier);
    }
    
    // Determine on which point we are going to branch.
    private List<BranchingDecision> branchOnOutliers(double[] outlier)
    {
    	for(int i=0; i<_instance.getPoints(); ++i)
    	{
    		if( outlier[i] >= _tolerance && outlier[i] <= 1 - _tolerance )
    		{
    			BranchOnOutlier branch1 = new BranchOnOutlier(_instance.getPoint(i), i, false);
    			BranchOnOutlier branch2 = new BranchOnOutlier(_instance.getPoint(i), i, true);

   				return Arrays.asList(branch1, branch2);
    		}
    	}
    		
 		return null;
    }    
}
