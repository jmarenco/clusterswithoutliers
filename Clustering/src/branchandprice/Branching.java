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
	
	// Branching candidate
	private int _pointIndex;
	private int _dimension;
	private double _threshold;
	private boolean _max;

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
		if( this.canPerformBranching(solution) == false )
			throw new RuntimeException("Cannot perform branching although the solution is fractional!");

		BranchingDecision branchingDecision1 = new BranchingDecision(_instance.getPoint(_pointIndex), _pointIndex, _dimension, _threshold, _max, true);
    	BranchingDecision branchingDecision2 = new BranchingDecision(_instance.getPoint(_pointIndex), _pointIndex, _dimension, _threshold, _max, false);

        return Arrays.asList(branchingDecision1, branchingDecision2);
    }

    // Determine on which point, dimension, threshold, and cluster side we are going to branch.
    private boolean canPerformBranching(Map<Cluster, Double> solution)
    {
    	// TODO: Se puede mejorar la complejidad?
    	for(int i=0; i<_instance.getPoints(); ++i)
    	{
    		Point point = _instance.getPoint(i);
    		List<Cluster> including = solution.keySet().stream().filter(c -> c.contains(point)).collect((Collectors.toList()));
        	
    		// TODO: Se pueden tener en cuenta todos los clusters al mismo tiempo?
    		if( including.size() >= 2 )
    		{
    			Cluster primero = including.get(0);
    			Cluster segundo = including.get(1);
    			
    			for(int t=0; t<_instance.getDimension(); ++t)
    			{
    				if( Math.abs(primero.max(t) - segundo.max(t)) > _tolerance )
	    			{
	    				_pointIndex = i;
	    				_dimension = t;
	    				_threshold = (primero.max(t) + segundo.max(t)) / 2;
	    				_max = true;

	    				return true;
	    			}

    				if( Math.abs(primero.min(t) - segundo.min(t)) > _tolerance )
	    			{
	    				_pointIndex = i;
	    				_dimension = t;
	    				_threshold = (primero.min(t) + segundo.min(t)) / 2;
	    				_max = false;

	    				return true;
	    			}
    			}
    		}
    	}
    		
 		return false;
    }
}
