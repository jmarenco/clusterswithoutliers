package incremental;

import java.util.HashSet;
import java.util.Set;

import general.Point;

public abstract class IncrementalManager 
{
	protected HashSet<Point> _unused_points;

	protected abstract void init();

	protected abstract Set<Point> getInitialPoints();

	protected abstract Set<Point> getNextSetOfPoints(Set<Integer> covered_by_last_solution);

}
