package branchandprice;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import general.Cluster;
import general.Instance;
import general.Point;

public class Branching
{
	// Instance
	private Instance _instance;

	// Tolerance for considering two cluster sides different
	private static double _tolerance = 0.01;

	// Constructor
    public Branching(Instance instance)
    {
        _instance = instance;
    }

    // Create the branches
    public List<BranchingDecision> getBranches(Map<Cluster, Double> solution)
    {
    	List<BranchingDecision> ret = branchOnSide(solution);
    	
    	if( ret != null )
    		return ret;
    	
		throw new RuntimeException("Cannot perform branching although the solution is fractional!");
    }

    // Determine on which point, dimension, threshold, and cluster side we are going to branch.
    private List<BranchingDecision> branchOnSide(Map<Cluster, Double> solution)
    {
    	// TODO: Se puede mejorar la complejidad?
    	for(int i=0; i<_instance.getPoints(); ++i)
    	{
    		Point point = _instance.getPoint(i);
    		List<Cluster> including = solution.keySet().stream().filter(c -> c.contains(point)).collect((Collectors.toList()));
    			
   			for(int tp=0; tp<_instance.getDimension(); ++tp)
   			{
   				int t = tp;
   				
   				double maxMax = including.stream().mapToDouble(c -> c.max(t)).max().orElse(0);
   				double minMax = including.stream().mapToDouble(c -> c.max(t)).min().orElse(0);
   				
   				if( maxMax - minMax > _tolerance )
    			{
   					BranchOnSide branch1 = new BranchOnSide(_instance.getPoint(i), i, t, (maxMax + minMax) / 2, true, true);
   					BranchOnSide branch2 = new BranchOnSide(_instance.getPoint(i), i, t, (maxMax + minMax) / 2, true, false);

   					return Arrays.asList(branch1, branch2);
    			}

   				double maxMin = including.stream().mapToDouble(c -> c.min(t)).max().orElse(0);
   				double minMin = including.stream().mapToDouble(c -> c.min(t)).min().orElse(0);

   				if( maxMin - minMin > _tolerance )
    			{
   					BranchOnSide branch1 = new BranchOnSide(_instance.getPoint(i), i, t, (maxMin + minMin) / 2, false, true);
   					BranchOnSide branch2 = new BranchOnSide(_instance.getPoint(i), i, t, (maxMin + minMin) / 2, false, false);

   					return Arrays.asList(branch1, branch2);
    			}
    		}
    	}
    		
 		return null;
    }
}
