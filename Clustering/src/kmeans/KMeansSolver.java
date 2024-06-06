package kmeans;

import java.util.ArrayList;
import java.util.Random;

import branchandprice.MasterWithRebuild;
import branchandprice.PricingZLRModel;
import branchandprice.Solver.Brancher;
import branchandprice.Solver.Pricer;
import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;
import interfaz.Viewer;

public class KMeansSolver 
{
	private static final Random random = new Random(0);
	
	private Instance _instance;
	
	private long _start;
	private long _iter;
	private double _ub;

	private Solution _solution;
	
	private static long _timeLimit = 3600;
	private static boolean _verbose = true;
	private static boolean _summary = false;

	public KMeansSolver(Instance instance) 
	{
		_instance = instance;
	}

	public static void setTimeLimit(long timeLimit)
	{
		_timeLimit = timeLimit;
	}
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}

	public Solution solve() 
	{
		_start = System.currentTimeMillis();
		_iter = 0;

		int k = _instance.getClusters();
		int n = _instance.getPoints();
		
		ArrayList<Point> centroids = randomCentroids();
	    int[] clusterOf = new int[n]; 
	    for (int i = 0; i < clusterOf.length; i++)
	    	clusterOf[i] = -1;

	    boolean changed = true;
	    while( changed && elapsedTime() < _timeLimit )
	    {
	    	++_iter;
	    	changed = false;
	        // in each iteration we should find the nearest centroid for each point
	    	for(int i=0; i<_instance.getPoints(); ++i)
	    	{
	    		Point point = _instance.getPoint(i);
	    		int cluster_index = nearestCentroid(point, centroids);
	    		if (cluster_index != clusterOf[i])
	    		{
	    			changed = true; 
		            clusterOf[i] = cluster_index; 
	    		}
	        }

	        // if the assignments do not change, then the algorithm terminates
	        if (!changed) 
	        { 
	            break; 
	        }
	        
//		    recoverSolution(clusterOf, true);

		    // at the end of each iteration we should relocate the centroids
	        relocateCentroids(centroids, clusterOf);
	    }
	    
	    recoverSolution(clusterOf);
	    
		if( _summary == true )
			showSummary();

		return _solution;
		
	}

	private void recoverSolution(int[] clusterOf)
	{
		int k = _instance.getClusters();
		_solution = new Solution();
	    for (int j = 0; j < k; j++)
	    	_solution.add(new Cluster());
	    
	    for (int i = 0; i < clusterOf.length; i++)
	    	_solution.addTo(_instance.getPoint(i), clusterOf[i]);
	}

	private void recoverSolution(int[] clusterOf, boolean showInViewer) 
	{
		recoverSolution(clusterOf);
		
	    if (showInViewer)
	    	new Viewer(_instance, _solution);
	}

	/**
	 * Recalculates the point corresponding to the centroid of each cluster.
	 * 
	 * @param centroids
	 * @param clusterOf
	 */
	private void relocateCentroids(ArrayList<Point> centroids, int[] clusterOf) 
	{
		int n = _instance.getPoints();
		for (int j = 0; j < _instance.getClusters(); j++)
		{
			Point centroid = null;
			int count = 0;
	    	for(int i=0; i < n; ++i)
	    	{
	    		if (clusterOf[i] == j)
	    		{
	    			count++;
	    			
	    			if (centroid == null)
	    				centroid = _instance.getPoint(i).clone();
	    			else
	    				centroid.sum(_instance.getPoint(i));
	    		}
	    	}
	    	
	    	if (centroid == null)
	    	{
	    		// TODO: empty clusters should be handled carefully! We should read a bit about this
	    		//		For now, I will just keep the old centroid for this cluster
	    		centroid = centroids.get(j);
	    	}
	    	else
	    		centroid.divide(count); 
			
	    	centroids.set(j, centroid);
			j++;
		}
	}


	/**
	 * Returns the nearest centroid for the given point
	 * @param point
	 * @param centroids
	 * @return
	 */
	private int nearestCentroid(Point point, ArrayList<Point> centroids) 
	{
		int nearest = -1;
		double minimumDistance = Double.MAX_VALUE;
		int i = 0;
		for (Point c : centroids) 
		{
			double currentDistance = point.distance(c);

	        if (currentDistance < minimumDistance) 
	        {
	            minimumDistance = currentDistance;
	            nearest = i;
	        }	
	        i++;
		}
		
		return nearest;
	}


	/**
	 * Generates a list of random centroids
	 * @return
	 */
	private ArrayList<Point> randomCentroids()
	{
		ArrayList<Point> centroids = new ArrayList<Point>();
		
		for (int i = 0; i < _instance.getClusters(); i++) 
		{
	        Point centroid = new Point(i, _instance.getDimension());
	        
	        for (int t = 0; t < _instance.getDimension(); t++) 
	        {
	            double max = _instance.max(t);
	            double min = _instance.min(t);
	            centroid.set(t, random.nextDouble() * (max - min) + min);
	        }

	        centroids.add(centroid);
	    }
		
		return centroids;
	}

	public double elapsedTime()
	{
		return (System.currentTimeMillis() - _start) / 1000.0;
	}
	
	private void showSummary()
	{
		double ub = _solution.calcObjVal();
		
		System.out.print(_instance.getDimension() + "\t");
		System.out.print(_instance.getPoints() + "\t");
		System.out.print(_instance.getClusters() + "\t");
		System.out.print(_instance.getOutliers() + "\t");
		System.out.print("KMN\t");
		System.out.print("Feasible\t");
		System.out.print(String.format("%6.4f", ub) + "\t");
		System.out.print(String.format("%6.2f", elapsedTime()) + "\t");
		System.out.print(_iter + "\t");

//		System.out.print(_instance.getName() + " | KMN | ");
//		System.out.print("Feasible | ");
//		System.out.print("Obj: " + String.format("%6.4f", ub) + " | ");
//		System.out.print(String.format("%6.2f", elapsedTime()) + " sec. | ");
//		System.out.print(_iter + " iter | ");
//		System.out.print("MT: " + _timeLimit + " | ");

		System.out.println();
	}	


}
