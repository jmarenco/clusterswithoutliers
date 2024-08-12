package incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;
import ilog.concert.IloException;

public class IncrementalSolver 
{
	private Instance _instance_base;
	private Instance _instance_cur;

	private Solution _last_solution;
	private HashSet<Integer> _covered_by_last_solution;
	
	private HashSet<Point> _unused_points;
	private Clock _clock;
	
	// Config
	private static boolean _verbose = true;
	private static boolean _summary = true;
	private static double _max_distance_to_neighbour = 0.1;
	private static int _increment_step = 10;

	public IncrementalSolver(Instance instance)
	{
		_instance_base = instance;
		_clock = new Clock();
		_last_solution = null;
	}
	
	public void setMaxTime(int maxTime) 
	{
		_clock.setTimelimit(maxTime);
	}

	public void solve() throws IloException
	{
		_clock.start();
		
		init();
		
		calculate_neighbourhoods();
		
		create_initial_instance();
		
		boolean solved = false;
		while (!solved)
		{
			solve_current();
			
			solved = check_solution();
			
			if (!solved)
			{
				add_points_to_current();
			}
		}
		
		_clock.stop();
		
		if (_summary)
		{
			System.out.print(_instance_base.getName() + " | Incr | ");
			System.out.print("Obj: " + String.format("%6.4f", _last_solution == null? "INF" : _last_solution.calcObjVal()) + " | ");
			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
			System.out.print("MD: " + _max_distance_to_neighbour + " | ");
			System.out.print("IS: " + _increment_step + " | ");
			System.out.print("MT: " + _clock.getTimeLimit() + " | ");
			System.out.println();
		}
	}

	private void init() 
	{
		IncrementalStandardModel.setVerbose(_verbose);
		IncrementalStandardModel.showSummary(false);
		
		_covered_by_last_solution = new HashSet<>();
	}

	private void calculate_neighbourhoods() 
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

	/**
	 * Creates an instance with only a subset of the points from the base instance.
	 * Also updates _unused_points set with all points not included in the initial instance.
	 */
	private void create_initial_instance() 
	{
		_instance_cur = new Instance(_instance_base.getClusters(), _instance_base.getOutliers());
		_unused_points = new HashSet<Point>();
		
//		// We will take only those points having excentricty 1
//		for (int i = 0; i < _instance_base.getPoints(); i++)
//		{
//			Point p = _instance_base.getPoint(i);
//			if (_neighbourhoods.get(p.getId()).getExcentricity() < 1)
//			{
//				_unused_points.add(p);
//			}
//			else
//			{
//				_instance_cur.add(p);
//			}
//		}
		
		// Another option: We use the same criteria as the used at each iteration
		for (int i = 0; i < _instance_base.getPoints(); i++)
			_unused_points.add(_instance_base.getPoint(i));
		
		add_points_to_current();
	}

	private void solve_current() throws IloException 
	{
		verb("Solving current instance of size " + _instance_cur.getPoints());
		
		IncrementalStandardModel model = new IncrementalStandardModel(_instance_cur);

		model.setMaxTime((int)_clock.remaining());
		model.setStrongBinding(false);
		_last_solution = model.solve();
	}

	private void verb(String string) 
	{
		if (_verbose)
			System.out.println(string);
	}

	private boolean check_solution() 
	{
		_covered_by_last_solution = new HashSet<>();
		int ncovered = 0;
		
		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			Point p = _instance_base.getPoint(i);
			
			for (Cluster cluster : _last_solution.getClusters())
			{
				if (cluster.covers(p)) // covered!
				{
					ncovered++;
					_covered_by_last_solution.add(p.getId());
					break;
				}
			}
		}
		
		verb("Solution has " + (_instance_base.getPoints() - ncovered) + " uncovered points [allowed = " + _instance_base.getOutliers() + "]" );
		
		return ncovered >= (_instance_base.getPoints() - _instance_base.getOutliers());
	}

	/**
	 * Selects a set of points from the _unused_points and adds these to _instance_cur.
	 * 
	 * From the _unused_points which are not covered by the last solution, it selects those 
	 * with the highest value of excentricity.
	 */
	private void add_points_to_current() 
	{
		// We first filter the unused points which are not covered by the previous slution
		ArrayList<Point> candidates = new ArrayList<>(_unused_points.size());
		for (Point p : _unused_points) if (!_covered_by_last_solution.contains(p.getId()))
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
			_instance_cur.add(p);
		}
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
			return (int) Math.signum(other._distanceExcentricity - _distanceExcentricity);
		}

		public double getExcentricity() { return _excentricity; }
		public double getDistanceExcentricity() { return _distanceExcentricity; }

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
				_excentricity = 1.0;
				_distanceExcentricity = Double.MAX_VALUE;
				return;
			}
			
			_excentricity = 0.0;
			_distanceExcentricity = 0.0;

			for (int t = 0; t < _p.getDimension(); ++t)
			{
				// For the excentricity
				double exc = Math.max(_pointsOnTheLeft[t], _pointsOnTheRight[t]);
				if (exc > _excentricity)
					_excentricity = exc;

				// For the distance-excentricity
				double dist_diff = 
						(exc == 1? // this means it has no neighbours on one of its sides (i.e., we will divide by 0)
								Double.MAX_VALUE
								:
								Math.abs((_distanceSumOnTheLeft[t]/_pointsOnTheLeft[t]) - (_distanceSumOnTheRight[t]/_pointsOnTheRight[t]))
						);
				if (dist_diff > _distanceExcentricity)
					_distanceExcentricity = dist_diff;
			}
			
			// For the excentricity
			_excentricity = _excentricity / _neighbours;
		}

	}

}
