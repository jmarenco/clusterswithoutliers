package branchandprice;

import java.util.Objects;

import general.Cluster;
import general.Instance;
import general.Point;

public class BranchRyanFoster implements BranchingDecision
{
	// Branching data
	private int _firstIndex;
	private int _secondIndex;
	private Point _firstPoint;
	private Point _secondPoint;
	private boolean _together;

    public BranchRyanFoster(Instance instance, int firstIndex, int secondIndex, boolean together)
    {
    	_firstIndex = firstIndex;
    	_secondIndex = secondIndex;
    	_firstPoint = instance.getPoint(firstIndex);
    	_secondPoint = instance.getPoint(secondIndex);
    	_together = together;
    }
    
    public int getFirstIndex()
    {
    	return _firstIndex;
    }
    
    public int getSecondIndex()
    {
    	return _secondIndex;
    }
    
    public Point getFirstPoint()
    {
    	return _firstPoint;
    }
    
    public Point getSecondPoint()
    {
    	return _secondPoint;
    }
    
    public boolean areTogether()
    {
    	return _together;
    }
    
    // Determine whether the given column remains feasible for the child node
    public boolean isCompatible(Column column)
    {
    	if( column.isArtificial() )
    		return true;

    	Cluster cluster = column.getCluster();
    	int points = (cluster.contains(_firstPoint) ? 1 : 0) + (cluster.contains(_secondPoint) ? 1 : 0);
    	
    	if( _together == true )
    		return points == 0 || points == 2;
    	else
    		return points == 0 || points == 1;
    }

    @Override
	public int hashCode()
	{
		return Objects.hash(_firstIndex, _secondIndex, _together);
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
		BranchRyanFoster other = (BranchRyanFoster) obj;
		return _firstIndex == other._firstIndex && _secondIndex == other._secondIndex && _together == other._together;
	}

	@Override
    public String toString()
    {
        return "RF(" + (_firstIndex+1) + ", " + (_secondIndex+1) + ", " + (_together ? "T" : "S") + ")";
    }
}
