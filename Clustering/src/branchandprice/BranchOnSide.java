package branchandprice;

import java.util.Objects;

import general.Cluster;
import general.Point;

public class BranchOnSide implements BranchingDecision
{
	// Branching data
	private Point _point;
	private int _pointIndex;
	private int _dimension;
	private double _threshold;
	private boolean _max;
	private boolean _lowerBound;
	
	// Tolerance for comparisons
	private static double _tolerance = 0.01;

    public BranchOnSide(Point point, int pointIndex, int dimension, double threshold, boolean max, boolean lowerBound)
    {
    	_point = point;
    	_pointIndex = pointIndex;
    	_dimension = dimension;
    	_threshold = threshold;
    	_max = max;
    	_lowerBound = lowerBound;
    }
    
    public int getPoint()
    {
    	return _pointIndex;
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
    public boolean isCompatible(Column column)
    {
    	if( column.isArtificial() )
    		return true;

    	boolean ret = true;
    	Cluster cluster = column.getCluster();
    	
    	if( cluster.contains(_point) == true )
    	{
    		double side = _max ? cluster.max(_dimension) : cluster.min(_dimension);
    		ret = _lowerBound ? side >= _threshold - _tolerance : side <= _threshold + _tolerance;
    	}
    	
//    	System.out.println("Compat " + cluster + " BC: " + this + " = " + ret);
    	return ret;
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
		BranchOnSide other = (BranchOnSide) obj;
		return _dimension == other._dimension && _lowerBound == other._lowerBound && _max == other._max
				&& _point == other._point
				&& Double.doubleToLongBits(_threshold) == Double.doubleToLongBits(other._threshold);
	}

    @Override
    public String toString()
    {
        return "BS(" + (_pointIndex+1) + ", " + _dimension + (_max ? ", Max " : ", Min ") + (_lowerBound ? ">= " : "<= ") + String.format("%.2f", _threshold) + ")";
    }
}
