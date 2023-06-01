package branchandprice;

import general.Cluster;
import general.Point;

public class BranchOnOutlier implements BranchingDecision
{
	// Branching data
	private Point _point;
	private int _pointIndex;
	private boolean _mustBeOutlier;

    public BranchOnOutlier(Point point, int pointIndex, boolean mustBeOutlier)
    {
    	_point = point;
    	_pointIndex = pointIndex;
    	_mustBeOutlier = mustBeOutlier;
    }
    
    public int getPoint()
    {
    	return _pointIndex;
    }
    
    public boolean mustBeOutlier()
    {
    	return _mustBeOutlier;
    }
    
    // Determine whether the given column remains feasible for the child node
    public boolean isCompatible(Column column)
    {
    	if( column.isArtificial() )
    		return true;

    	Cluster cluster = column.getCluster();
    	return cluster.contains(_point) == false || _mustBeOutlier == false;
    }
    
    @Override
	public int hashCode()
    {
		final int prime = 31;
		int result = 1;
		result = prime * result + (_mustBeOutlier ? 1231 : 1237);
		result = prime * result + _pointIndex;
		return result;
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
		BranchOnOutlier other = (BranchOnOutlier) obj;
		if (_mustBeOutlier != other._mustBeOutlier)
			return false;
		if (_pointIndex != other._pointIndex)
			return false;
		return true;
	}

	@Override
    public String toString()
    {
        return "Branch on outlier " + (_pointIndex+1) + ", outlier: " + _mustBeOutlier;
    }
}
