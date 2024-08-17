package incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import general.Instance;
import general.Point;
import incremental.IncrementalSolver.Metric;

public class EccentricityManager extends IncrementalManager 
{
	private HashMap<Integer, Neighbourhood> _neighbourhoods;
	private Instance _instance_base;
	
	private static double _max_distance_to_neighbour = 0.2;
	private static int _increment_step = 20;
	private static boolean _initial_different_calc = true;
	private static boolean _sum_over_dimensions = false;

	public EccentricityManager(Instance ins) 
	{
		_instance_base = ins;
	}

	@Override
	protected String method() 
	{
		return (IncrementalSolver.incrementalMetric == Metric.Eccentricity ? "ECC_" : "DIST_")
				+ (_sum_over_dimensions? "SUM_" : "MAX_")
				+ _max_distance_to_neighbour + "_" + _increment_step;
	}

	@Override
	protected void init() 
	{
		_neighbourhoods = new HashMap<>();
		
		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			Point p = _instance_base.getPoint(i);
			Neighbourhood N = new Neighbourhood(p);
			
			// For each other point in p's neighbourhood we check if its either in N- or N+ 
			for (int j = 0; j < _instance_base.getPoints(); j++) if (i != j)
			{
				Point p2 = _instance_base.getPoint(j);
				
				if (p.distance(p2) <= _max_distance_to_neighbour) // is in the neighbourhood
					N.add_neighbour(p2);
			}
			
			N.calculate_excentricity();
			
			System.out.println("(Dist-)Excentricity of point " + p.getId() + " = (" + N.getDistanceExcentricity() + ") " + N.getExcentricity() + " [" + N.getNsize()  + " neighs]");
			
			_neighbourhoods.put(p.getId(), N);
		}		
	}

	@Override
	protected Set<Point> getInitialPoints() 
	{
		_unused_points = new HashSet<Point>();
		
		if (_initial_different_calc)
		{
			HashSet<Point> initial_points = new HashSet<>();

			// We take the most (dist-)eccentric points
			ArrayList<Point> candidates = new ArrayList<>(_instance_base.getPoints());
			for (int i = 0; i < _instance_base.getPoints(); i++)
				candidates.add(_instance_base.getPoint(i));

			// Now we sort the list by excentricity
			Collections.sort(candidates, (p1, p2) -> 
				_neighbourhoods.get(p1.getId()).compareTo(_neighbourhoods.get(p2.getId())));
			
//			for (Point p : candidates)
//			{
//				System.out.println("Point " + p.getId() + " --> " + _neighbourhoods.get(p.getId()).getExcentricity());
//			}
			
			// We now take the first points on this sorted list
			double best = _neighbourhoods.get(candidates.get(0).getId()).getCurrentExcentricity();
			for (int i = 0; i < candidates.size(); i++)
			{
				Point p = candidates.get(i);
				if (_neighbourhoods.get(p.getId()).getCurrentExcentricity() >= best * 0.9)
				{
					initial_points.add(p);
				}
				else
					_unused_points.add(p);
			}

//			// Another option: We will take only those points having excentricty >= 1
//			for (int i = 0; i < _instance_base.getPoints(); i++)
//			{
//				Point p = _instance_base.getPoint(i);
//				if (_neighbourhoods.get(p.getId()).getExcentricity() < 1)
//				{
//					_unused_points.add(p);
//				}
//				else
//				{
//					initial_points.add(p);
//				}
//			}
			
			return initial_points;
		}
		else 
		{
			for (int i = 0; i < _instance_base.getPoints(); i++)
				_unused_points.add(_instance_base.getPoint(i));
			
			return getNextSetOfPoints(new HashSet<Integer>());
		}
	}

	@Override
	protected Set<Point> getNextSetOfPoints(Set<Integer> covered_by_last_solution) 
	{
		HashSet<Point> new_points = new HashSet<>();

		// We first filter the unused points which are not covered by the previous slution
		ArrayList<Point> candidates = new ArrayList<>(_unused_points.size());
		for (Point p : _unused_points) if (!covered_by_last_solution.contains(p.getId()))
			candidates.add(p);

		// Now we sort the list by excentricity
		Collections.sort(candidates, (p1, p2) -> 
			_neighbourhoods.get(p1.getId()).compareTo(_neighbourhoods.get(p2.getId())));
		
//		for (Point p : candidates)
//		{
//			System.out.println("Point " + p.getId() + " --> " + _neighbourhoods.get(p.getId()).getExcentricity());
//		}
		
		// We now take the first points on this sorted list
		for (int i = 0; i < _increment_step && i < candidates.size(); i++)
		{
			Point p = candidates.get(i);
			_unused_points.remove(p);
			new_points.add(p);
		}
		
		return new_points;
	}


	public static void setSumOverDimensions(boolean sum) 
	{
		_sum_over_dimensions = sum;
	}

	public static void setMaxDistanceToNeighbour(double max) 
	{
		_max_distance_to_neighbour = max;
	}

	public static void setIncrementStep(int inc_step) 
	{
		_increment_step = inc_step;
	}

	
	/**
	 * Keeps data of the neighbourhod of a point
	 * @author diego
	 *
	 */
	static public class Neighbourhood 
	{
		Point _p;
		private int[] _pointsOnTheRight;
		private int[] _pointsOnTheLeft;
		private double _neighbours;
		
		private double[] _distanceSumOnTheRight;
		private double[] _distanceSumOnTheLeft;

		private double _excentricity;
		private double _distanceExcentricity;
		
		public Neighbourhood(Point p) 
		{
			_p = p;
			_pointsOnTheRight = new int[p.getDimension()];
			_pointsOnTheLeft = new int[p.getDimension()];
			_distanceSumOnTheRight = new double[p.getDimension()];
			_distanceSumOnTheLeft = new double[p.getDimension()];
			_neighbours = 0.0;
			
			_excentricity = 0.0;
			_distanceExcentricity = 0.0;
		}

		public int getNsize() 
		{	
			return  (int) _neighbours;
		}

		public void add_neighbour(Point p2) 
		{
			for (int t = 0; t < _p.getDimension(); t++)
			{
				if (_p.get(t) < p2.get(t))
				{
					_pointsOnTheRight[t]++;
					_distanceSumOnTheRight[t] += Math.abs(_p.get(t) - p2.get(t));
				}
				else
				{
					_pointsOnTheLeft[t]++;
					_distanceSumOnTheLeft[t] += Math.abs(_p.get(t) - p2.get(t));
				}
			}
			_neighbours++;
		}

		public int compareTo(Neighbourhood other) 
		{
//			return (int) Math.signum(other._excentricity - _excentricity);
//			return (int) Math.signum(other._distanceExcentricity - _distanceExcentricity);
			return (int) Math.signum(other.getCurrentExcentricity() - getCurrentExcentricity());
		}

		public double getExcentricity() { return _excentricity; }
		public double getDistanceExcentricity() { return _distanceExcentricity; }
		public double getCurrentExcentricity() 
		{
			return IncrementalSolver.incrementalMetric == Metric.Eccentricity ? getExcentricity() : getDistanceExcentricity();
//			return getDistanceExcentricity();
		}

		/**
		 * The excentricty on a coordinate d is measured by:
		 * 
		 * 		E_t(x) = max{|N_t^-(x)|, |N_t^+(x)|} / |N(x)|
		 * 
		 * The global excentricty is then calculated as:
		 * 		E(x) = max{ E_t(x) : t = 1, ..., D }, with D being the dimensions.
		 * 
		 */
		public void calculate_excentricity() 
		{
			if (_neighbours == 0)
			{
				_excentricity = _sum_over_dimensions ? _p.getDimension() : 1.0;
				_distanceExcentricity = _p.getDimension() * _max_distance_to_neighbour;
				return;
			}
			
			_excentricity = 0.0;
			_distanceExcentricity = 0.0;

			for (int t = 0; t < _p.getDimension(); ++t)
			{
				// For the excentricity
				double exc = Math.max(_pointsOnTheLeft[t], _pointsOnTheRight[t]) / (double) _neighbours;
				
				if (_sum_over_dimensions)
					_excentricity += exc;
				else if (exc > _excentricity)
					_excentricity = exc;

				// For the distance-excentricity
				double dist_diff = 
						(exc == 1? // this means it has no neighbours on one of its sides (i.e., we will divide by 0)
								_max_distance_to_neighbour
								:
								Math.abs((_distanceSumOnTheLeft[t]/_pointsOnTheLeft[t]) - (_distanceSumOnTheRight[t]/_pointsOnTheRight[t]))
						);

				if (_sum_over_dimensions)
					_distanceExcentricity += dist_diff;
				else if (dist_diff > _distanceExcentricity)
					_distanceExcentricity = dist_diff;
			}
		}

	}

}
