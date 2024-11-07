package incremental;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;

public class FeasibleSolutionHeuristic {
	
	private Instance _ins;
	private double[] _minDistanceToCluster;
	private int[] _closest_cluster;

	private int _ndim;
	private int _npoints;
	
	public FeasibleSolutionHeuristic(Instance ins) 
	{
		_ins = ins;
		_ndim = _ins.getDimension();
		_npoints = ins.getPoints();
		_minDistanceToCluster = new double[_npoints];
		_closest_cluster = new int[_npoints];
	}
	
	
	public Solution MakeFeasible(Solution original_solution)
	{
		ArrayList<Cluster> clusters = original_solution.getClusters();
		// Create a priority queue w.r.t minimum distance to clusters.
		ComputeDistanceToClusters(clusters);
        PriorityQueue<Integer> pq = new PriorityQueue<Integer>(_npoints, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(_minDistanceToCluster[b],_minDistanceToCluster[a]);
            }
        });
		Set<Integer> in_queue = new HashSet<Integer>();
		for (int i=0; i< _npoints; i++) {
			if (_minDistanceToCluster[i] > 0) {
				pq.offer(i);
				in_queue.add(i);
			}
        }
		// Main loop until all points are covered.
		while (!pq.isEmpty()) {
			int point_to_include = pq.poll();
			int closest_cluster = _closest_cluster[point_to_include];
			Point point = _ins.getPoint(point_to_include);
			_closest_cluster[point_to_include] = closest_cluster;
			_minDistanceToCluster[point_to_include] = 0.0;
			clusters.get(closest_cluster).add(point);
			// Update distance of remaining points.
			Iterator<Integer> iterator = in_queue.iterator();
			while (iterator.hasNext()) {
			    Integer p = iterator.next();
			    double newClusterDistance = DistanceToCluster(_ins.getPoint(p), clusters.get(closest_cluster));
			    if (_minDistanceToCluster[p] > newClusterDistance) {
			    	_closest_cluster[p] = closest_cluster;
			    	_minDistanceToCluster[p] = newClusterDistance;
			    	pq.remove(p);
			    	if (newClusterDistance > 0) {
			    		pq.offer(p);
			    	} else {
						clusters.get(closest_cluster).add(_ins.getPoint(p));
			    		iterator.remove();
			    	}
			    }
			}
		}
		return new Solution(clusters);
	}
	
	private double DistanceToCluster(Point p, Cluster c) {
		double distance = 0.0;
		for (int t=0; t<_ndim; t++)
		{
			distance += Math.max(p.get(t) - c.max(t), 0.0);
			distance += Math.max(c.min(t) - p.get(t), 0.0);
		}
		return distance;
	}
	
	private void ComputeDistanceToClusters(ArrayList<Cluster> clusters) {
		for (int i=0; i< _npoints; i++) {
			_minDistanceToCluster[i] = DistanceToCluster(_ins.getPoint(i), clusters.get(0));
			_closest_cluster[i] = 0;
			for (int j=1; j<clusters.size(); j++) {
				double distance = DistanceToCluster(_ins.getPoint(i), clusters.get(j));
				if (distance < _minDistanceToCluster[i]) {
					_minDistanceToCluster[i] = distance;
					_closest_cluster[i] = j;
				}
			}
		}
	}
}
