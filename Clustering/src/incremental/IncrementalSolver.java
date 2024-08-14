package incremental;

import java.util.HashSet;
import java.util.Set;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Point;
import general.Solution;
import ilog.concert.IloException;
import incremental.IncrementalSolver.Metric;
import interfaz.Viewer;
import standardModel.RectangularModel;

public class IncrementalSolver 
{
	private Instance _instance_base;
	private Instance _instance_cur;

	private Solution _last_solution;
	private HashSet<Integer> _covered_by_last_solution;
	
	private Clock _clock;
	
	private IncrementalManager _incrementalManager;
	
	// Config
	private static boolean _verbose = true;
	private static boolean _summary = true;
	
	public static enum Metric { None, Random, Eccentricity, DistanceEccentricity, CorePoints }; 
	public static Metric separationMetric = Metric.DistanceEccentricity;

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
		
		init_manager();
		
		create_initial_instance();
		
		boolean solved = false;
		while (!solved)
		{
			solve_current();
			
			solved = check_solution();
			
			if (!solved)
			{
				add_points_to_current(_incrementalManager.getNextSetOfPoints(_covered_by_last_solution));
			}
		}
		
		_clock.stop();
		
		if (_summary)
		{
			System.out.print(_instance_base.getName() + " | Incr | ");
			System.out.print("Obj: " + String.format("%6.4f", _last_solution == null? "INF" : _last_solution.calcObjVal()) + " | ");
			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
//			System.out.print("MD: " + _max_distance_to_neighbour + " | ");
//			System.out.print("IS: " + _increment_step + " | ");
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

	private void init_manager() 
	{
		if (IncrementalSolver.separationMetric == Metric.Eccentricity || IncrementalSolver.separationMetric == Metric.DistanceEccentricity)
			_incrementalManager = new EccentricityManager(_instance_base);
		else if (IncrementalSolver.separationMetric == Metric.Random)
			_incrementalManager = new RandomManager(_instance_base);

		_incrementalManager.init();
	}

	/**
	 * Creates an instance with only a subset of the points from the base instance.
	 * Also updates _unused_points set with all points not included in the initial instance.
	 */
	private void create_initial_instance() 
	{
		_instance_cur = new Instance(_instance_base.getClusters(), _instance_base.getOutliers());
		
		add_points_to_current(_incrementalManager.getInitialPoints());
	}

	private void solve_current() throws IloException 
	{
		verb("Solving current instance of size " + _instance_cur.getPoints());
		
//		IncrementalStandardModel model = new IncrementalStandardModel(_instance_cur);
		RectangularModel model = new RectangularModel(_instance_cur);

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
		
		new Viewer(_instance_base, _last_solution);
		
		return ncovered >= (_instance_base.getPoints() - _instance_base.getOutliers());
	}

	/**
	 * Selects a set of points from the _unused_points and adds these to _instance_cur.
	 * 
	 * From the _unused_points which are not covered by the last solution, it selects those 
	 * with the highest value of excentricity.
	 * @param new_points 
	 */
	private void add_points_to_current(Set<Point> new_points) 
	{
		for (Point p : new_points)
		{
			_instance_cur.add(p);
		}
	}

	public Set<Integer> covered_by_last_solution() 
	{
		return _covered_by_last_solution;
	}

	public static void setMetric(String metric) 
	{
		if (metric.toUpperCase().equals("NONE"))
			separationMetric = Metric.None;
		else if (metric.toUpperCase().equals("RAND"))
			separationMetric = Metric.Random;
		else if (metric.toUpperCase().equals("ECC"))
			separationMetric = Metric.Eccentricity;
		else if (metric.toUpperCase().equals("DIST"))
			separationMetric = Metric.DistanceEccentricity;
		else if (metric.toUpperCase().equals("CORE"))
			separationMetric = Metric.CorePoints;
		else
		{
			String msg = "[IncrementalSolver] The metric " + metric + " is not a valid metric.";
			System.out.println(msg);
			System.out.println("Options are: NONE | RANDOM | ECC | DISTECC | CORE");
			throw new RuntimeException(msg);
		}
	}
}
