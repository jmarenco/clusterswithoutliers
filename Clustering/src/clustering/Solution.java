package clustering;

import java.util.ArrayList;

public class Solution
{
	private ArrayList<Cluster> _clusters;
	
	public Solution(Master master)
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
