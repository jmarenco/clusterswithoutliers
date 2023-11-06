package branchandprice;

import java.util.List;
import general.Cluster;

public abstract class Pricing
{
	// Main method for solving the pricing problem
    public abstract List<Cluster> generateColumns(double timeLimit);

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public abstract void updateObjective();

    // Close the pricing problem
    public abstract void close();

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public abstract void performBranching(BranchingDecision sc);
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public abstract void reverseBranching(BranchingDecision sc);
    
    // Gets total solving time
    public abstract double getSolvingTime();
    
    // Gets number of generated columns
    public abstract int getGeneratedColumns();
    
    // Maximum number of columns to generate per pricing
    private static int _max_cols_per_pricing = 1;
    public static void setMaxColsPerPricing(int max) { _max_cols_per_pricing = max; }
    public int getMaxColsPerPricing() { return _max_cols_per_pricing; }
}
