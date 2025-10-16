package incremental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import general.Instance;
import general.Point;
import incremental.IncrementalSolver.Metric;

public class RandomManager extends IncrementalManager 
{
	private Instance _instance_base;
	private Random _random;

	private static int _increment_step = 20;
	private static double _probability_to_add = 0.1;
	private static int _random_seed = 0;

	public RandomManager(Instance ins) 
	{
		_instance_base = ins;
	}
	
	@Override
	protected String method() 
	{
		return "RAND_" + _probability_to_add + "_" + _increment_step;
	}

	@Override
	protected void init() 
	{
		_unused_points = new HashSet<Point>();
		_random = new Random(_random_seed);
	}

	@Override
	protected Set<Point> getInitialPoints() 
	{
//		HashSet<Point> initial_points = new HashSet<>();
//		
//		// We take random points with the given threshold
//		for (int i = 0; i < _instance_base.getPoints(); i++)
//		{
//			Point p = _instance_base.getPoint(i);
//			if (_random.nextDouble() < _random_threshold)
//			{
//				initial_points.add(p);
////				verb("      ++ Point " + (p.getId()-1) + " [id=" + p.getId() + "]");
//			}
//			else
//				_unused_points.add(p);
//		}
//		
//		return initial_points;

		for (int i = 0; i < _instance_base.getPoints(); i++)
			_unused_points.add(_instance_base.getPoint(i));
		
		return getNextSetOfPoints(new HashSet<Integer>());
	}

	@Override
	protected Set<Point> getNextSetOfPoints(Set<Integer> covered_by_last_solution) 
	{
		HashSet<Point> new_points = new HashSet<>();

		// We first filter the unused points which are not covered by the previous slution
		ArrayList<Point> candidates = new ArrayList<>(_unused_points.size());
		for (Point p : _unused_points) if (!covered_by_last_solution.contains(p.getId()))
			candidates.add(p);

		// We take randomly up to _increment_step points (but at least one!)
		int count = 0;
		double chance = Math.max(_probability_to_add, (double)_increment_step/(double)candidates.size()); // so in average we add inc_step
		for (int i = 0; count < _increment_step && i < candidates.size(); i++) 
			if (_random.nextDouble() < chance)
			{
				Point p = candidates.get(i);
				_unused_points.remove(p);
				new_points.add(p);
//				verb("         >> Covering point " + p.getId());
				++count;
			}
		
		if (count == 0) // We need to take at least 1 point!
		{
			Point p = candidates.get(_random.nextInt(candidates.size()));
			_unused_points.remove(p);
			new_points.add(p);
//			verb("         >> Covering point " + p.getId());
			++count;
		}
		
		return new_points;
	}
	
	public static void setIncrementStep(int inc_step) 
	{
		_increment_step = inc_step;
	}
	
	public static void setAddingProbability(double add_prob) 
	{
		if (add_prob < 0 || add_prob > 1)
			throw new RuntimeException("Probability for the random incremental must be in [0,1]!");
			
		_probability_to_add = add_prob;
	}

	public static void setSeed(int seed) 
	{
		_random_seed = seed;
	}
}
