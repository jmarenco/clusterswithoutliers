package incremental;

import java.util.HashSet;
import java.util.Set;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Logger;
import general.Point;
import general.Results;
import general.Results.Status;
import general.Solution;
import ilog.concert.IloException;
import incremental.IncrementalSolver.Metric;
import interfaz.Viewer;
import standardModel.RectangularModel;

public class IncrementalSolver 
{
	private Instance _instance_base;
	private Instance _instance_cur;

	private Solution _last_solution = null;
	private Solution _best_incumbent = null;
	private double _best_lb = 0.0;
	private double _best_ub = Double.MAX_VALUE;
	private HashSet<Integer> _covered_by_last_solution;
	
	private Clock _clock;
	
	private IncrementalManager _incrementalManager;
	private RectangularModel _model;
	
	// Config
	private static boolean _verbose = true;
	private static boolean _summary = true;
	private static boolean _show_intermediate_solutions = false;
	private double myEpsilon = 0.000001;
	
	public static enum Metric { None, Random, Eccentricity, DistanceEccentricity, BorderPoints }; 
	public static Metric incrementalMetric = Metric.DistanceEccentricity;

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

	public static void setShowIntermediateSolutions(boolean show) 
	{
		_show_intermediate_solutions = show;
	}

	public void solve() throws IloException
	{
		_clock.start();
		
		init();
		
		init_manager();
		
		create_initial_instance();
		
		boolean solved = false;
		int iter = 0;
		while (!solved && !_clock.timeout())
		{
			solve_current();
			
			solved = check_solution();
			
			if (!solved)
			{
				try_to_improve_incumbent();
				
				if (_best_lb + myEpsilon >= _best_ub)
					solved = true;
				
				if (!solved)
					add_points_to_current(_incrementalManager.getNextSetOfPoints(_covered_by_last_solution));
			}

			iter++;
			closeModel();
		}
		
		_clock.stop();
		
		if (_summary)
		{
			System.out.print(_instance_base.getName() + " | " + method() + " | ");
			System.out.print("LB: " + String.format("%6.4f", _best_lb) + " | ");
			System.out.print("UB: " + String.format("%6.4f", _best_incumbent == null? "INF" : _best_ub) + " | ");
			System.out.print("GAP: " + String.format("%6.2f", 100.0*(_best_ub - _best_lb) / _best_lb) + "% | ");
			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
			System.out.print("MT: " + _clock.getTimeLimit() + " | ");
			System.out.println();
		}
		
		// Log the solution
		Results.Status stat = _best_incumbent == null? Status.NOSOLUTION : (_best_lb + myEpsilon < _best_ub  ? Status.FEASIBLE : Status.OPTIMAL);
		Logger.log(_instance_base, method(), new Results(_last_solution, stat, _best_lb, _best_ub, _clock.elapsed(), -1, iter, _instance_cur.getPoints()));
	}

	private void closeModel() 
	{
		_model.closeSolver();
	}

	private String method() 
	{
		return "INC_" + _incrementalManager.method();
	}

	private void init() 
	{
		IncrementalStandardModel.setVerbose(_verbose);
		IncrementalStandardModel.showSummary(false);
		
		_covered_by_last_solution = new HashSet<>();
	}

	private void init_manager() 
	{
		if (IncrementalSolver.incrementalMetric == Metric.Eccentricity || IncrementalSolver.incrementalMetric == Metric.DistanceEccentricity)
			_incrementalManager = new EccentricityManager(_instance_base);
		else if (IncrementalSolver.incrementalMetric == Metric.Random)
			_incrementalManager = new RandomManager(_instance_base);
		else if (IncrementalSolver.incrementalMetric == Metric.BorderPoints)
			_incrementalManager = new BorderPointsManager(_instance_base);

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
		_model = new RectangularModel(_instance_cur);
		_model.setLogSolution(false);
		_model.keepAlive();

		_model.setMaxTime((int) Math.ceil(_clock.remaining()));
		_model.setStrongBinding(false);

//		OBS: I tried setting a LB on the objective function, but it seems this is not a good practice (in general).
//		See for example: 
//		https://or.stackexchange.com/questions/4264/how-to-use-tight-upper-and-lower-bounds-to-get-to-the-optimal-value-via-branch-a
//		if (_best_lb > 0.0)
//			_model.setObjLB(_best_lb);
		
		_last_solution = _model.solve();
		_best_lb = Math.max(_best_lb, _model.getLastLB());
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
		
		if (_show_intermediate_solutions)
			new Viewer(_instance_base, _last_solution, "" + _last_solution.calcObjVal());
		
		
		boolean feasible = ncovered >= (_instance_base.getPoints() - _instance_base.getOutliers());
		
		if (feasible)
		{
			double ub = _last_solution.calcObjVal();
			
			if (ub < _best_ub)
			{
				_best_ub = ub;
				_best_incumbent = _last_solution;
			}
		}
		
		return feasible;
	}

	/**
	 * Tries to improve the incumbent in order to improve the UB.
	 * @throws IloException 
	 */
	private void try_to_improve_incumbent() throws IloException 
	{
		// For the moment, we will ask the model for the previous integer solutions found
		// with the hope that some of it covers the entire set of points.
		// Note that solutions are consulted in increasing order by obj function, so as 
		// soon as we found a (globally) feasible one, we can take it and stop the search.
		
		int n = _model.getNSolutions();
		
		for (int s = 1; s <= n; s++)
		{
			Solution sol = _model.getSolutionNumber(s);
			double sol_ub = _model.getObjValueOfSolutionN(s);
			
			if (sol_ub >= _best_ub) // Done... no more interesting solutions here
				break;
			else if (isFeasible(sol)) // Super! We found an improvement
			{
				System.out.println(" >>>>>> Incumbent improved with suboptimal solutions from previous iteration! [" + _best_ub + " -> " + sol_ub  + "]");
				
				_best_ub = sol_ub;
				_best_incumbent = sol;

				// We stop here because all subsequent solutions are worst or equal
				break;
			}
		}
	}

	private boolean isFeasible(Solution sol) 
	{
		int ncovered = 0;
		
		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			Point p = _instance_base.getPoint(i);
			
			for (Cluster cluster : sol.getClusters())
			{
				if (cluster.covers(p)) // covered!
				{
					ncovered++;
					break;
				}
			}
		}
		
		return ncovered >= (_instance_base.getPoints() - _instance_base.getOutliers());	
	}

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
			incrementalMetric = Metric.None;
		else if (metric.toUpperCase().equals("RAND"))
			incrementalMetric = Metric.Random;
		else if (metric.toUpperCase().equals("ECC"))
			incrementalMetric = Metric.Eccentricity;
		else if (metric.toUpperCase().equals("DIST"))
			incrementalMetric = Metric.DistanceEccentricity;
		else if (metric.toUpperCase().equals("BORD"))
			incrementalMetric = Metric.BorderPoints;
		else
		{
			String msg = "[IncrementalSolver] The metric " + metric + " is not a valid metric.";
			System.out.println(msg);
			System.out.println("Options are: NONE | RANDOM | ECC | DISTECC | BORD");
			throw new RuntimeException(msg);
		}
	}
}
