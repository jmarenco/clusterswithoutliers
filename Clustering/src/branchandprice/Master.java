package branchandprice;

import java.util.ArrayList;
import java.util.Map;
import general.Instance;
import general.Cluster;

public interface Master
{
    // Solve the master problem
    public boolean solve(long timeLimit);

    // Extracts information from the master problem required by the pricing problems
    public double[] getDuals();
    
    // Adds columns ensuring that a feasible solution exists
    public void addFeasibleColumns();

    // Adds a new column to the master problem
    public void addColumn(Cluster cluster);

    // Gets the solution from the master problem. Returns all non-zero valued columns from the master problem
    public Map<Cluster, Double> getSolution();

    // Gets the values for the y-variables
    public double[] getOutliers();

    // Gets the objective value of the last solution
    public double getObjValue();

    // Informs whether the last solution is optimal
    public boolean isOptimal();
    
    // Informs whether the last solution is feasible (no artificial variables with non-null values)
    public boolean isFeasible();
    
    // Informs whether the last solution is integer
    public boolean isIntegerSolution();

    // Closes the master problem
    public void close();

    // Listen to branching decisions
    public void performBranching(BranchingDecision bd);

    // Undo branching decisions during backtracking in the Branch-and-Price tree
    public void reverseBranching(BranchingDecision bd);

    // Gets instance
    public Instance getInstance();

    // Gets all columns
    public ArrayList<Column> getColumns();
}