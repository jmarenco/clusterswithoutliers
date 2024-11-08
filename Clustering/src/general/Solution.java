package general;

import java.util.ArrayList;

import colgen.MasterCovering;
import colgen.MasterPacking;

public class Solution
{
	private ArrayList<Cluster> _clusters;
	
	public Solution(MasterPacking master)
	{
		_clusters = new ArrayList<Cluster>();
		
		for(int i=0; i<master.getClusters().size(); ++i) if( master.getPrimal(i) > 0.9 )
		{
			Cluster cluster = new Cluster();
			for(Point point: master.getClusters().get(i).asSet())
				cluster.add(point);
			
			_clusters.add(cluster);
		}
	}
	
	public Solution(MasterCovering master)
	{
		_clusters = new ArrayList<Cluster>();
		
		for(int i=0; i<master.getClusters().size(); ++i) if( master.getPrimal(i) > 0.9 )
		{
			Cluster cluster = new Cluster();
			for(Point point: master.getClusters().get(i).asSet())
				cluster.add(point);
			
			_clusters.add(cluster);
		}
	}

	public Solution(ArrayList<Cluster> clusters)
	{
		_clusters = clusters;
	}

	public Solution() 
	{
		_clusters = new ArrayList<>();
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}

	public void add(Cluster cluster) 
	{
		_clusters.add(cluster);
	}

	public Cluster get(int i) 
	{
		return _clusters.get(i);
	}

	public void addTo(Point point, int j) 
	{
		_clusters.get(j).add(point);
	}
	
	public boolean isFeasible()
	{
		return _clusters != null;
	}

	public double calcObjVal() 
	{
		double span = 0;
		
		for (Cluster c : _clusters)
			span += c.totalSpan();
		
		return span;
	}

	public static Solution withAllPoints(Instance instance) 
	{
		Solution ret = new Solution();
		ret.add(Cluster.withAllPoints(instance));
		return ret;
	}
	
}
