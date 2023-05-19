package branchandprice;

import java.util.Objects;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

// Branching constraint
public final class ConstraintOnClusterSide implements BranchingDecision<InputData, PotentialCluster>
{
	// Branching data
	private int _point;
	private int _dimension;
	private double _threshold;
	private boolean _max;
	private boolean _lowerBound;
	
	// Tolerance for comparisons
	private static double _tolerance = 0.01;

    public ConstraintOnClusterSide(int point, int dimension, double threshold, boolean max, boolean lowerBound)
    {
    	_point = point;
    	_dimension = dimension;
    	_threshold = threshold;
    	_max = max;
    	_lowerBound = lowerBound;
    }
    
    public int getPoint()
    {
    	return _point;
    }
    
    public int getDimension()
    {
    	return _dimension;
    }
    
    public double getThreshold()
    {
    	return _threshold;
    }
    
    public boolean appliesToMaxSide()
    {
    	return _max;
    }
    
    public boolean isLowerBound()
    {
    	return _lowerBound;
    }
    
    // Determine whether the given column remains feasible for the child node
    @Override
    public boolean columnIsCompatibleWithBranchingDecision(PotentialCluster column)
    {
    	double side = _max ? column.getCluster().max(_dimension) : column.getCluster().min(_dimension);
    	return _lowerBound ? side >= _threshold - _tolerance : side <= _threshold + _tolerance;
    }

    // Determine whether the given inequality remains feasible for the child node
    @Override
    public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality)
    {
        return true; // Cuts are not added in this example
    }

	@Override
	public int hashCode()
	{
		return Objects.hash(_dimension, _lowerBound, _max, _point, _threshold);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConstraintOnClusterSide other = (ConstraintOnClusterSide) obj;
		return _dimension == other._dimension && _lowerBound == other._lowerBound && _max == other._max
				&& _point == other._point
				&& Double.doubleToLongBits(_threshold) == Double.doubleToLongBits(other._threshold);
	}

    @Override
    public String toString()
    {
        return "Branch on point " + _point + ", dim: " + _dimension + (_max ? " - Max " : " - Min ") + (_lowerBound ? "<= " : ">= ") + _threshold;
    }
}