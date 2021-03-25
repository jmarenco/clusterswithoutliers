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
}
