package general;

import java.util.HashSet;
import java.util.Objects;
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
	
	public static Cluster fromSet(Instance instance, Set<Integer> indexes)
	{
		Cluster ret = new Cluster();
		
		for(Integer i: indexes)
			ret.add(instance.getPoint(i));
		
		return ret;
	}
	
	public static Cluster withAllPoints(Instance instance)
	{
		Cluster ret = new Cluster();
		
		for(int i=0; i<instance.getPoints(); ++i)
			ret.add(instance.getPoint(i));
		
		return ret;
	}
	
	public static Cluster singleton(Point point)
	{
		Cluster ret = new Cluster();
		ret.add(point);
		return ret;
	}

	public void add(Point point)
	{
		_points.add(point);
	}
	
	public void remove(Point point)
	{
		_points.remove(point);
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
	
	public double totalDistanceToCentroid()
	{
		if( _points.size() == 0 )
			return 0;
		
		Point c = this.centroid();
		return _points.stream().mapToDouble(p -> p.distance(c)).sum();
	}
	
	public double totalSpan()
	{
		if( _points.size() == 0 )
			return 0;
		
		int dimension = _points.iterator().next().getDimension();

		double ret = 0;
		for(int t=0; t<dimension; ++t)
			ret += span(t);
		
		return ret;
	}
	
	public double span(int dimension)
	{
		return max(dimension) - min(dimension);
	}
	
	public double max(int dimension)
	{
		return _points.stream().mapToDouble(p -> p.get(dimension)).max().orElse(0);
	}
	
	public double min(int dimension)
	{
		return _points.stream().mapToDouble(p -> p.get(dimension)).min().orElse(0);
	}
	
	private Point centroid()
	{
		Point ret = null;
		
		for(Point point: _points)
		{
			if (ret == null)
				ret = point.clone();
			else
				ret.sum(point);
		}
		
		if (_points.size() > 0)
			ret.divide(_points.size());
		
		return ret;
	}
	
	public Set<Point> getPoints()
	{
		return _points;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(_points);
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
		Cluster other = (Cluster) obj;
		return Objects.equals(_points, other._points);
	}

	@Override public String toString()
	{
		String ret = "";
		for(Point point: _points)
			ret += (ret.length() > 0 ? ", " : "") + point.getId();
		
		return "{" + ret + "}";
	}
}
