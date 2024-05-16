package branchandprice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import general.Cluster;
import general.Instance;
import general.Point;

public class BranchingRyanFoster extends Branching
{
	// Tolerance for considering two cluster sides different
	private static double _tolerance = 0.01;

	// Constructor
    public BranchingRyanFoster(Instance instance)
    {
    	super(instance);
    }

    // Create the branches
    public List<BranchingDecision> getBranches(Map<Cluster, Double> solution, double[] outlier)
    {
    	List<BranchingDecision> ret = branchRyanFoster(solution);
    	
    	if( ret != null )
    		return ret;
    	
    	ret = branchOnOutliers(outlier);
    	
    	if( ret != null )
    		return ret;

    	throw new RuntimeException("Cannot perform branching although the solution is fractional!");
    }

    // Determine on which pair of points we are going to branch.
    private List<BranchingDecision> branchRyanFoster(Map<Cluster, Double> solution)
    {
    	// TODO: Se puede mejorar la complejidad?
    	for(int i=0; i<_instance.getPoints(); ++i)
       	for(int j=i+1; j<_instance.getPoints(); ++j)
    	{
    		Point first = _instance.getPoint(i);
    		Point second = _instance.getPoint(j);

    		double lhs = solution.keySet().stream().filter(c -> c.contains(first) && c.contains(second)).mapToDouble(c -> solution.get(c)).sum();
    		
    		if( _tolerance <= lhs && lhs <= 1-_tolerance )
    		{
    			BranchRyanFoster branch1 = new BranchRyanFoster(_instance, i, j, true);
    			BranchRyanFoster branch2 = new BranchRyanFoster(_instance, i, j, false);

				return Arrays.asList(branch1, branch2);
   			}
    	}
    		
 		return null;
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
