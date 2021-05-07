package clustering;

import java.util.ArrayList;

public class Instance
{
	private ArrayList<Point> _points;
	private int _clusters;
	private int _outliers;
	
	public Instance(int clusters, int outliers)
	{
		_points = new ArrayList<Point>();
		_clusters = clusters;
		_outliers = outliers;
	}
	
	public void add(Point p)
	{
		if( _points.size() > 0 && _points.get(0).getDimension() != p.getDimension() )
			throw new RuntimeException("Input points have different dimensions!");
		
		_points.add(p);
	}
	
	public int getPoints()
	{
		return _points.size();
	}
	
	public Point getPoint(int i)
	{
		if (i < 0 || i >= getPoints())
			throw new RuntimeException("Out of range point index: " + i);
		
		return _points.get(i);
	}
	
	public int getClusters()
	{
		return _clusters;
	}
	
	public int getOutliers()
	{
		return _outliers;
	}
	
	public int getDimension()
	{
		return _points.size() == 0 ? 0 : _points.get(0).getDimension();
	}
	
	public double min(int coordinate)
	{
		return _points.stream().mapToDouble(p -> p.get(coordinate)).min().orElse(0) - 1;
	}
	
	public double max(int coordinate)
	{
		return _points.stream().mapToDouble(p -> p.get(coordinate)).max().orElse(0) + 1;
	}
}
