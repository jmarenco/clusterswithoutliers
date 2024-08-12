package incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import general.Instance;
import general.Point;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.cplex.IloCplex;

public class LazyCoveringSeparator implements LazySeparatorInterface
{
	private static final double EPS = 0.00001;
	
	private LazySeparator _parent;
	private IncrementalStandardModel _model;
	
	private Instance _instance_base;
	
	private int _npoints;
	private int _nclusters;
	private int _ndimensions;
	private int _noutliers;
	
	private HashSet<Point> _initial_points;
	private HashSet<Point> _unused_points;
	private HashMap<Integer, Neighbourhood> _neighbourhoods;
	private HashSet<Integer> _covered_by_last_solution;
	
	private Random _random;

	// Config
	private static boolean _verbose = true;
	private static double _max_distance_to_neighbour = 0.3;
	private static int _increment_step = 1;
	private double _random_threshold = 0.1;
	private int _random_seed = 0;
	
	public static Metric separationMetric = Metric.DistanceEccentricity;
	public static enum Metric { None, Random, Eccentricity, DistanceEccentricity, CorePoints }; 


	public LazyCoveringSeparator(LazySeparator lazySeparator, Instance instance) 
	{
		_parent = lazySeparator;
		_model = (IncrementalStandardModel) _parent.getModelInterface();
		_instance_base = instance;
		_npoints = _instance_base.getPoints();
		_nclusters = _instance_base.getClusters();
		_ndimensions = _instance_base.getDimension();
		_noutliers = _instance_base.getOutliers();
		
		_covered_by_last_solution = new HashSet<>();
		_random = new Random(_random_seed);
		
		calculate_neighbourhoods();
		create_initial_instance();
	}
	
	public static void setMetric(String metric) 
	{
		if (metric.toUpperCase().equals("NONE"))
			separationMetric = Metric.None;
		else if (metric.toUpperCase().equals("RANDOM"))
			separationMetric = Metric.Random;
		else if (metric.toUpperCase().equals("ECC"))
			separationMetric = Metric.Eccentricity;
		else if (metric.toUpperCase().equals("DISTECC"))
			separationMetric = Metric.DistanceEccentricity;
		else if (metric.toUpperCase().equals("CORE"))
			separationMetric = Metric.CorePoints;
		else
		{
			String msg = "[LazyCoveringSeparator] The metric " + metric + " is not a valid metric.";
			System.out.println(msg);
			System.out.println("Options are: NONE | RANDOM | ECC | DISTECC | CORE");
			throw new RuntimeException(msg);
		}
	}

	private void verb(String string) 
	{
		if (_verbose)
			System.out.println(string);
	}
	
	private void calculate_neighbourhoods() 
	{
		_neighbourhoods = new HashMap<>();
		
		for (int i = 0; i < _npoints; i++)
		{
			Point p = _instance_base.getPoint(i);
			Neighbourhood N = new Neighbourhood(p);
			
			// For each other point in p's neighbourhood we check if its either in N- or N+ 
			for (int j = 0; j < _npoints; j++) if (i != j)
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
	 * Creates the set of initial points to be covered from the start.
	 * Also updates _unused_points set with all points not included in the initial set.
	 */
	private void create_initial_instance() 
	{
		_initial_points = new HashSet<Point>();
		_unused_points = new HashSet<Point>();
		
		verb(" ++ Creating Initial points:");
		if (separationMetric == Metric.Eccentricity || separationMetric == Metric.DistanceEccentricity)
		{
			// We will take only those points having excentricty 1
			for (int i = 0; i < _npoints; i++)
			{
				Point p = _instance_base.getPoint(i);
				if (_neighbourhoods.get(p.getId()).getExcentricity() < 1)
				{
					_unused_points.add(p);
				}
				else
				{
					_initial_points.add(p);
					verb("      ++ Point " + (p.getId()-1) + " [id=" + p.getId() + "]");
				}
			}
		}
		else if (separationMetric == Metric.Random)
		{
			// We take random points with the given threshold
			for (int i = 0; i < _npoints; i++)
			{
				Point p = _instance_base.getPoint(i);
				if (_random.nextDouble() < _random_threshold)
				{
					_initial_points.add(p);
					verb("      ++ Point " + (p.getId()-1) + " [id=" + p.getId() + "]");
				}
				else
					_unused_points.add(p);
			}
		}
		
		_model.setInitialSetOfPointsToBeCovered(_initial_points);
	}

	public void separate() throws IloException
	{
		verb("Executing Lazy Covering Constraint separator!");
		
		checkSolution();
		
		int uncovered = _npoints - _covered_by_last_solution.size();
		verb("Solution has " + uncovered + " uncovered points [allowed = " + _noutliers + "]");
		
		if (uncovered > _noutliers)
		{
			// We first filter the unused points which are not covered by the previous slution
			ArrayList<Point> candidates = new ArrayList<>(_unused_points.size());
			for (Point p : _unused_points) if (!_covered_by_last_solution.contains(p.getId()))
				candidates.add(p);

			if (separationMetric == Metric.Eccentricity || separationMetric == Metric.DistanceEccentricity)
			{
				// Now we sort the list by excentricity
				if (separationMetric == Metric.Eccentricity)
					Collections.sort(candidates, (p1, p2) -> 
						_neighbourhoods.get(p1.getId()).compareEccentricityTo(_neighbourhoods.get(p2.getId())));
				else if (separationMetric == Metric.DistanceEccentricity)
					Collections.sort(candidates, (p1, p2) -> 
						_neighbourhoods.get(p1.getId()).compareDistanceEccentricityTo(_neighbourhoods.get(p2.getId())));
			
				verb("    >> Applying Lazy constraints [" + candidates.size() + " candidate points]:");
	//			for (Point p : candidates)
	//			{
	//				System.out.println("Point " + p.getId() + " --> " + _neighbourhoods.get(p.getId()).getExcentricity());
	//			}
				
				// We now take the first points on this sorted list
				for (int i = 0; i < _increment_step && i < candidates.size(); i++)
				{
					Point p = candidates.get(i);
					_unused_points.remove(p);
					addCut(p);
					verb("         >> Covering point " + (p.getId()-1) + " [id=" + p.getId() + "]");
				}
			}
			else if (separationMetric == Metric.Random)
			{
				// We take randomly up to _increment_step points (but at least one!)
				int count = 0;
				for (int i = 0; count < _increment_step && i < candidates.size(); i++) 
					if (_random.nextDouble() < _random_threshold)
					{
						Point p = candidates.get(i);
						_unused_points.remove(p);
						addCut(p);
						verb("         >> Covering point " + p.getId());
						++count;
					}
				
				if (count == 0) // We need to take at least 1 point!
				{
					Point p = candidates.get(_random.nextInt(candidates.size()));
					_unused_points.remove(p);
					addCut(p);
					verb("         >> Covering point " + p.getId());
					++count;
				}
			}
		}
		
		verb("Used/Unused points to this point: " + (_npoints - _unused_points.size()) + " / " + _unused_points.size());
	}

	/**
	 * Adds the covering cut for the given point.
	 * 
	 * @param p
	 * @throws IloException 
	 */
	private void addCut(Point p) throws IloException 
	{
		if (_model.getStrongBinding() == true)
		{
		    for(int j=0; j<_nclusters; ++j)
			for(int t=0; t<_ndimensions; ++t)
			{
				IloCplex master = _model.getCplex();
				IloNumExpr inequality = master.linearNumExpr();

				inequality = master.sum(inequality, _model.lVar(j, t));
				inequality = master.sum(inequality, master.prod(- p.get(t) + _instance_base.max(t), _model.zVar(p.getId()-1, j)));
				_parent.addCut( master.le(inequality, _instance_base.max(t)));
//				master.addLe(inequality, _instance_base.max(t));
			}
		    
		    for(int j=0; j<_nclusters; ++j)
			for(int t=0; t<_ndimensions; ++t)
			{
				IloCplex master = _model.getCplex();
				IloNumExpr inequality = master.linearNumExpr();

				inequality = master.sum(inequality, _model.rVar(j, t));
				inequality = master.sum(inequality, master.prod(- p.get(t) + _instance_base.min(t), _model.zVar(p.getId()-1, j)));
				_parent.addCut( master.ge(inequality, _instance_base.min(t)));
//				master.addGe(inequality, _instance_base.min(t));
			}			
		}
		else // Weak binding
		{
		    for(int j=0; j<_nclusters; ++j)
			for(int t=0; t<_ndimensions; ++t)
			{
				IloCplex master = _model.getCplex();
				IloNumExpr inequality = master.linearNumExpr();

				inequality = master.sum(inequality, _model.lVar(j, t));
				inequality = master.sum(inequality, master.prod(_instance_base.max(t) - _instance_base.min(t), _model.zVar(p.getId()-1, j)));

				_parent.addCut( master.le(inequality, p.get(t) + _instance_base.max(t) - _instance_base.min(t)));
//				master.addLe(inequality, p.get(t) + _instance_base.max(t) - _instance_base.min(t));
			}
		    
		    for(int j=0; j<_nclusters; ++j)
			for(int t=0; t<_ndimensions; ++t)
			{
				IloCplex master = _model.getCplex();
				IloNumExpr inequality = master.linearNumExpr();

				inequality = master.sum(inequality, _model.rVar(j, t));
				inequality = master.sum(inequality, master.prod(- _instance_base.max(t) + _instance_base.min(t), _model.zVar(p.getId()-1, j)));
				_parent.addCut( master.ge(inequality, p.get(t) - _instance_base.max(t) + _instance_base.min(t)));
//				master.addGe(inequality, p.get(t) - _instance_base.max(t) + _instance_base.min(t));
			}
		}
		
	}


	/**
	 * Checks which points are covered by the solution and stores it in _covererd_by_last_solution.
	 * 
	 * @throws IloException
	 */
	private void checkSolution() throws IloException 
	{
		_covered_by_last_solution = new HashSet<Integer>();
		for (int i = 0; i < _npoints; i++)
		{
			Point p = _instance_base.getPoint(i);
			for (int j = 0; j < _nclusters; j++) 
			{
				boolean covered = true;
				for (int t = 0; t < _ndimensions; ++t)
				{
					if (_parent.lVar(j, t) > p.get(t) + EPS || _parent.rVar(j, t) < p.get(t) - EPS) // out of the box!
					{
						covered = false;
						
						// DBG
						if (_parent.zVar(i, j) > 0.5 && !_unused_points.contains(p))
						{
							// This cannot happen
							double left = _parent.lVar(j, t);
							double right = _parent.rVar(j, t);
							double val = _parent.zVar(i, j);
							double point = p.get(t);
							
							verb("Point " + i + "[id=" + p.getId() + "] is in the model with Z = 1 but actually uncovered by the solution!");
						}
						
						break;
					}
				}

				if (covered)
				{
					_covered_by_last_solution.add(p.getId());
					break;
				}
			}
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

		public int compareEccentricityTo(Neighbourhood other) 
		{
			return (int) Math.signum(other._excentricity - _excentricity);
		}

		public int compareDistanceEccentricityTo(Neighbourhood other) 
		{
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
