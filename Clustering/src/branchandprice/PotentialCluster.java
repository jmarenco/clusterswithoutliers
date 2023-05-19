package branchandprice;

import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;
import general.Cluster;
import java.util.Set;

// Definition of a column
public final class PotentialCluster extends AbstractColumn<InputData, ClusteringPricingProblem>
{
	@Deprecated
    public final Set<Integer> vertices;
    
    private final Cluster _cluster;
    public final double cost; // Cost of this column in the objective of the Master Problem

    @Deprecated
    public PotentialCluster(ClusteringPricingProblem associatedPricingProblem, boolean isArtificial, String creator, Set<Integer> vertices, double cost)
    {
        super(associatedPricingProblem, isArtificial, creator);
        this.vertices=vertices;
        this.cost=cost;
        _cluster = null;
    }

    public PotentialCluster(ClusteringPricingProblem associatedPricingProblem, boolean isArtificial, String creator, Cluster cluster)
    {
        super(associatedPricingProblem, isArtificial, creator);
        vertices = null;
        _cluster = cluster;
        this.cost = cluster.totalSpan();
    }
    
    public Cluster getCluster()
    {
    	return _cluster;
    }

    @Override
    public boolean equals(Object o)
    {
        if(this==o)
            return true;
        else if(!(o instanceof PotentialCluster))
            return false;
        PotentialCluster other=(PotentialCluster) o;
        return this.getCluster().equals(other.getCluster()) && this.isArtificialColumn == other.isArtificialColumn;
    }

    @Override
    public int hashCode()
    {
        return _cluster.hashCode();
    }

    @Override
    public String toString()
    {
        return "Value: " + this.value + ", artificial: " + isArtificialColumn + ", cluster: " + _cluster.toString();
    }
}