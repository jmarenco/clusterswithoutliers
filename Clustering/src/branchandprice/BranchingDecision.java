package branchandprice;

public interface BranchingDecision
{
    // Determine whether the given column remains feasible for the child node
    public boolean isCompatible(Column column);
}
