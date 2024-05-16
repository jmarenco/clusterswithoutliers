package branchandprice;

import java.util.List;
import java.util.Map;

import general.Cluster;
import general.Instance;

public abstract class Branching
{
	// Instance
	protected Instance _instance;

	// Constructor
    public Branching(Instance instance)
    {
        _instance = instance;
    }

    // Create the branches
    public abstract List<BranchingDecision> getBranches(Map<Cluster, Double> solution, double[] outlier);
}
