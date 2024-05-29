package branchandprice;

import general.Cluster;
import general.Instance;
import general.Point;

public class Column
{
	private Cluster _cluster;
	private double _cost;
	private boolean _artificial;
	
	private Column(Cluster cluster, double cost, boolean artificial)
	{
		_cluster = cluster;
		_cost = cost;
		_artificial = artificial;
	}
	
	public static Column artificial(Instance instance)
	{
		return new Column(Cluster.withAllPoints(instance), 1000, true);
	}
	
	public static Column regular(Cluster cluster)
	{
		return new Column(cluster, cluster.totalSpan(), false);
	}
	
	public static Column singleton(Point point)
	{
		return new Column(Cluster.singleton(point), 0, false);
	}
	
	public Cluster getCluster()
	{
		return _cluster;
	}
	
	public double getCost()
	{
		return _cost;
	}
	
	public boolean isArtificial()
	{
		return _artificial;
	}
	
	public boolean contains(Point point)
	{
		return _cluster.contains(point);
	}
	
	@Override
	public String toString()
	{
		return _cluster.toString() + (_artificial ? " [artificial]" : "");
	}
}
