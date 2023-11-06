package branchandprice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import general.Cluster;
import general.Instance;
import general.NearestNeighbors;

public class PricingHeuristic extends Pricing
{
	// Input data
	private Master _master;
	private Instance _instance;
	private NearestNeighbors _neighbors;
	private Random _random;
	private int p; // Points
	private double[] dualCosts;

	// Statistics
	private double _solvingTime = 0;
	private int _generatedColumns = 0;
	
	// Creates a new solver instance for a particular pricing problem
    public PricingHeuristic(Master master)
    {
    	_master = master;
        _instance = master.getInstance();
        _neighbors = new NearestNeighbors(_instance);
        _random = new Random(0);
        
		p = _instance.getPoints();
    }
	
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit)
    {
        List<Cluster> ret = new ArrayList<>();
        double start = System.currentTimeMillis();
        
        for(int i=0; i<p; ++i) if( _random.nextDouble() < 0.2 )
        {
        	Cluster cluster = constructFrom(i);
        	if( cluster != null && ret.contains(cluster) == false )
        		ret.add(cluster);
        }

        _solvingTime += (System.currentTimeMillis() - start) / 1000.0;
        _generatedColumns += ret.size();
        
        return ret;
    }
    
    private Cluster constructFrom(int origin)
    {
    	double pointDuals = dualCosts[origin];
    	double currentDual = -pointDuals;

    	Cluster ret = Cluster.fromArray(_instance, origin);
    	int[] neighbors = _neighbors.get(origin);
    	
    	for(int i=0; i<neighbors.length; ++i)
    	{
    		int j = neighbors[i];
    		ret.add(_instance.getPoint(j));

    		double newDual = ret.totalSpan() - pointDuals - dualCosts[j];
    		if( currentDual > newDual )
    		{
    			currentDual = newDual;
    			pointDuals += dualCosts[j];
    		}
    		else
    		{
    			ret.remove(_instance.getPoint(j));
    		}
    	}
    	
    	return ret.totalSpan() - pointDuals + dualCosts[p] < -0.01 ? ret : null;
    }

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective()
    {
    	dualCosts = _master.getDuals();
    }

    // Close the pricing problem
    public void close()
    {
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc)
    {
    	throw new RuntimeException("Branching decision applied on root pricing!");
    }
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc)
    {
    	throw new RuntimeException("Branching decision applied on root pricing!");
    }
    
    public double getSolvingTime()
    {
    	return _solvingTime;
    }
    
    public int getGeneratedColumns()
    {
    	return _generatedColumns;
    }
}
