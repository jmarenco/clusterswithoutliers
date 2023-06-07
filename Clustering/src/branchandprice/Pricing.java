package branchandprice;

import java.util.List;
import general.Cluster;

public interface Pricing
{
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit);

    // Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective();

    // Close the pricing problem
    public void close();

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc);
    
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc);
    
    // Gets total solving time
    public double getSolvingTime();
    
    // Gets number of generated columns
    public int getGeneratedColumns();
}
