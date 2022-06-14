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

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
}
