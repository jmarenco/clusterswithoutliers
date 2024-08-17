package incremental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import general.Instance;
import general.Point;

public class BorderPointsManager extends IncrementalManager 
{
	private Instance _instance_base;
	
	private HashMap<Integer, HashSet<Integer>> _neighbours;
	private int[] _neighbourcount;
	
	private static double _max_distance_to_neighbour = 0.2;
	private static double _min_neighbours_for_being_core = 5;
	private static int _increment_step = 20;

	public BorderPointsManager(Instance ins) 
	{
		_instance_base = ins;
	}

	@Override
	protected String method() 
	{
		return "BORD_" + _max_distance_to_neighbour + "_" + _increment_step;
	}

	@Override
	protected void init() 
	{
		_neighbours = new HashMap<Integer, HashSet<Integer>>();
		_neighbourcount = new int[_instance_base.getPoints()]; // +1 is because point ids are 1-indexed

		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			_neighbours.put(i, new HashSet<Integer>());
			_neighbourcount[i] = 0;
		}

		for (int i = 0; i < _instance_base.getPoints(); i++)
		{
			Point p = _instance_base.getPoint(i);

			// For each other point check if it is in p's neighbourhoods 
			for (int j = i+1; j < _instance_base.getPoints(); j++)
			{
				Point p2 = _instance_base.getPoint(j);
				
				if (p.distance(p2) <= _max_distance_to_neighbour)
				{
					_neighbours.get(i).add(j);
					_neighbours.get(j).add(i);

					_neighbourcount[i]++;
					_neighbourcount[j]++;
				}
			}
		}		
	}

	@Override
	protected Set<Point> getInitialPoints() 
	{
		_unused_points = new HashSet<Point>();
		
		HashSet<Point> initial_points = new HashSet<>();

		ArrayList<Point> candidates = new ArrayList<>(_instance_base.getPoints());
		for (int i = 0; i < _instance_base.getPoints(); i++)
			candidates.add(_instance_base.getPoint(i));

		// From these we take the all points with few neighbours
		// First we sort the list by number of neighbouts
		Collections.sort(candidates, (p1, p2) -> (int) Math.signum(_neighbourcount[p1.getId()-1] - _neighbourcount[p2.getId()-1]));
		
		for (Point p : candidates)
		{
			System.out.println("Point " + p.getId() + " " + p.toString() + " --> " + _neighbourcount[p.getId()-1]);
		}
		
		// We now take the first points on this sorted list
		double best = _neighbourcount[candidates.get(0).getId()-1];
		for (int i = 0; i < candidates.size(); i++)
		{
			Point p = candidates.get(i);
			if (_neighbourcount[p.getId()-1] <= best * 1.2)
			{
				initial_points.add(p);
			}
			else
				_unused_points.add(p);
		}
		
		// We update the neighbourhood counts
		update_count(initial_points);
		
		return initial_points;
	}

	/**
	 * Receives a set of points which will be passed to the solver. Updates the 
	 * neighbour_count of every unused point by removing the points in the given set.
	 * @param points
	 */
	private void update_count(HashSet<Point> points) 
	{
		for (Point p : points)
		{
			for (Integer i : _neighbours.get(p.getId()-1))
			{
				_neighbourcount[i]--;
			}
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

		// From these we take the all points with few neighbours
		// First we sort the list by number of neighbouts
		Collections.sort(candidates, (p1, p2) -> (int) Math.signum(_neighbourcount[p1.getId()-1] - _neighbourcount[p2.getId()-1]));
		
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
		
		// We update the neighbourhood counts
		update_count(new_points);
		
		return new_points;
	}

	public static void setMaxDistanceToNeighbour(double max) 
	{
		_max_distance_to_neighbour = max;
	}

	public static void setMinNeighboursForBeingCore(double max) 
	{
		_min_neighbours_for_being_core = max;
	}

	public static void setIncrementStep(int inc_step) 
	{
		_increment_step = inc_step;
	}
}
