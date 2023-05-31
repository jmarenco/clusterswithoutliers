package jorlib;

import org.jorlib.frameworks.columnGeneration.colgenMain.AbstractColumn;
import general.Cluster;

// Definition of a column
public final class PotentialCluster extends AbstractColumn<InputData, ClusteringPricingProblem>
{
    private Cluster _cluster;
    private double _cost; // Cost of this column in the objective of the Master Problem

    public PotentialCluster(ClusteringPricingProblem associatedPricingProblem, boolean artificial, String creator, Cluster cluster)
    {
        super(associatedPricingProblem, artificial, creator);

        _cluster = cluster;
        _cost = artificial ? 10000 : cluster.totalSpan(); // Tiene que ser entero
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
    
    public double getCost()
    {
    	return _cost;
    }

    @Override
    public String toString()
    {
        return "Value: " + this.value + ", artificial: " + isArtificialColumn + ", cluster: " + _cluster.toString();
    }
}