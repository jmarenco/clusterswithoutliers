package incremental;

import java.util.HashSet;
import java.util.List;
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
import standardModelCpsat.RectangularModelCpsat;

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
//	private RectangularModel _model;
//	private RectangularModelCpsat _model;
	private BlackBoxClusteringSolver _bbsolver;
	
	// Config
	private static boolean _verbose = true;
	private static boolean _summary = true;
	private static boolean _show_intermediate_solutions = false;
	private double myEpsilon = 0.000001;
	
	public static enum Metric { None, Random, Eccentricity, DistanceEccentricity, BorderPoints }; 
	public static Metric incrementalMetric = Metric.DistanceEccentricity;

	public static enum Solver { CompactModel, CPSAT, BAPSolver }; 
	public static Solver solverModel = Solver.CompactModel;

	public IncrementalSolver(Instance instance)
	{
		_instance_base = instance;
		_clock = new Clock();
		_last_solution = null;
		
		_best_incumbent = Solution.withAllPoints(instance);
		_best_ub = _best_incumbent.calcObjVal();
	}
	
	public void setMaxTime(int maxTime) 
	{
		_clock.setTimelimit(maxTime);
	}

	public static void setShowIntermediateSolutions(boolean show) 
	{
		_show_intermediate_solutions = show;
	}

	public void solve() throws IloException, Exception
	{
		_clock.start();
		
		init();
		
		init_bbsolver();
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
				
				if (currentGap() < myEpsilon) // gap is 0!
					solved = true;
				else
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
			System.out.print("UB: " + _best_incumbent == null? "INF" : String.format("%6.4f",_best_ub) + " | ");
			System.out.print("GAP: " + String.format("%6.2f", 100.0*currentGap()) + "% | ");
			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
			System.out.print("MT: " + _clock.getTimeLimit() + " | ");
			System.out.print("BB: " + _bbsolver.getSolverName() + " | ");
			System.out.println();
		}
		
		// Log the solution
		Results.Status stat = _best_incumbent == null? Status.NOSOLUTION : (currentGap() < myEpsilon? Status.OPTIMAL : Status.FEASIBLE);
		Logger.log(_instance_base, method(), new Results(_last_solution, stat, _best_lb, _best_ub, _clock.elapsed(), -1, iter, _instance_cur.getPoints()));
	}

	private double currentGap() 
	{
		return (_best_ub - _best_lb)/ (_best_lb + myEpsilon);
	}

	private void closeModel() 
	{
		_bbsolver.closeSolver();
	}

	private String method() 
	{
		return "INC_" + _incrementalManager.method();
	}

	private void init() 
	{
		RectangularLazyIncrementalModel.setVerbose(_verbose);
		RectangularLazyIncrementalModel.showSummary(false);
		
		_covered_by_last_solution = new HashSet<>();
	}

	private void init_bbsolver() 
	{
		if (IncrementalSolver.solverModel == Solver.CompactModel)
		{
			RectangularModel model = new RectangularModel(_instance_base);
			model.setLogSolution(false);
			model.keepAlive();
			model.setStrongBinding(false);
			
			_bbsolver = model;
		}
		else if (IncrementalSolver.solverModel == Solver.CPSAT)
		{
			RectangularModelCpsat model = new RectangularModelCpsat(_instance_base);
			model.setLogSolution(false);
			
			_bbsolver = model;
		}
		else if (IncrementalSolver.solverModel == Solver.BAPSolver)
		{
			// TODO
			_bbsolver = null;
		}
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

	private void solve_current() throws IloException, Exception
	{
		verb("Solving current instance of size " + _instance_cur.getPoints());
		
		_bbsolver.setMaxTime((int) Math.ceil(_clock.remaining()));
		_last_solution = _bbsolver.solve(_instance_cur);
		_best_lb = Math.max(_best_lb, _bbsolver.getLastLB());
	}

	private void verb(String string) 
	{
		if (_verbose)
			System.out.println(string);
	}

	/**
	 * Recovers the best solution found on the last iteration and checks if it is feasible for the global instance.
	 * At the same time, it calculates the set of points covered by this solution (to use this set later, to select 
	 * more points to add to the instance.
	 * 
	 * @return
	 */
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
		
		if (feasible) // Then we know it is optimal for the global instance! 
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
	private void try_to_improve_incumbent() throws IloException, Exception
	{
		// For the moment, we will ask the model for the integer solutions found during 
		// last solve, with the hope that some of it covers the entire set of points.
		// Note that solutions are consulted in increasing order by obj function, so as 
		// soon as we found a (globally) feasible one, we can take it and stop the search.
		
		int n = _bbsolver.getNSolutions();
		
		for (int s = 1; s < n; s++)
		{
			double sol_obj = _bbsolver.getObjValueOfSolutionN(s);
			if (sol_obj >= _best_ub) // Done... no more interesting solutions here (assuming they are sorted by obj value)
				break;
			else
			{
				Solution sol = _bbsolver.getSolutionNumber(s);

				if (isFeasible(sol)) // Super! We found an improvement
				{
					System.out.println(" >>>>>> Incumbent improved with suboptimal solutions from previous iteration! [" + _best_ub + " -> " + sol_obj  + "]");
					
					_best_ub = sol_obj;
					_best_incumbent = sol;
	
					// We stop here because all subsequent solutions are worst or equal
					break;
				}
			}
		}
	}

	/**
	 * Checks if the given solution is feasible for the global instance (i.e., if it covers enough points).
	 * 
	 * @param sol
	 * @return
	 */
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
	
	public static void setBBSolver(String bbs) 
	{
		if (bbs.toUpperCase().equals("SM"))
			solverModel = Solver.CompactModel;
		else if (bbs.toUpperCase().equals("CPSAT"))
			solverModel = Solver.CPSAT;
		else if (bbs.toUpperCase().equals("BAP"))
			solverModel = Solver.BAPSolver;
		else
		{
			String msg = "[IncrementalSolver] The BB solver " + bbs + " is not a valid BB solver.";
			System.out.println(msg);
			System.out.println("Options are: SM | CPSAT | BAP");
			throw new RuntimeException(msg);
		}
	}

}
