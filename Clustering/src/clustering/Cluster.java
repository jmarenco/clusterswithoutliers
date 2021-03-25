package clustering;

import java.util.HashSet;
import java.util.Set;

public class Cluster
{
	private Set<Point> _points;
	
	public Cluster()
	{
		_points = new HashSet<Point>();
	}
	
	public static Cluster fromArray(Instance instance, int... indexes)
	{
		Cluster ret = new Cluster();
		
		for(Integer i: indexes)
			ret.add(instance.getPoint(i));
		
		return ret;
	}
	
	public void add(Point point)
	{
		_points.add(point);
	}
	
	public Set<Point> asSet()
	{
		return _points;
	}
	
	public int size()
	{
		return _points.size();
	}
	
	public boolean contains(Point point)
	{
		return _points.contains(point);
	}
	
	public double objective()
	{
		if( _points.size() == 0 )
			return 0;
		
		Point c = this.centroid();
		return _points.stream().mapToDouble(p -> p.distance(c)).sum();
	}
	
	private Point centroid()
	{
		Point ret = null;
		
		for(Point point: _points)
		{
			if (ret == null)
				ret = point;
			else
				ret.sum(point);
		}
		
		if (_points.size() > 0)
			ret.divide(_points.size());
		
		return ret;
	}
}
