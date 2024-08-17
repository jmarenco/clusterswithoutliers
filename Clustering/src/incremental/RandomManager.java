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

	public static int increment_step = 20;
	private double _random_threshold = 0.1;
	private int _random_seed = 0;

	public RandomManager(Instance ins) 
	{
		_instance_base = ins;
	}
	
	@Override
	protected String method() 
	{
		return "RAND_" + _random_threshold + "_" + increment_step;
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
		HashSet<Point> initial_points = new HashSet<>();
		
		// We take random points with the given threshold
		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			Point p = _instance_base.getPoint(i);
			if (_random.nextDouble() < _random_threshold)
			{
				initial_points.add(p);
//				verb("      ++ Point " + (p.getId()-1) + " [id=" + p.getId() + "]");
			}
			else
				_unused_points.add(p);
		}
		
		return initial_points;
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
		for (int i = 0; count < increment_step && i < candidates.size(); i++) 
			if (_random.nextDouble() < _random_threshold)
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
}
