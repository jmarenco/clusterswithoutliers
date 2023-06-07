package general;

import java.util.ArrayList;
import java.util.Collections;

// Computes the nearest neighbors of each point
public class NearestNeighbors
{
	private Instance _instance;
	private ArrayList<int[]> _neighbors;
	
	public NearestNeighbors(Instance instance)
	{
		_instance = instance;
		_neighbors = new ArrayList<int[]>(_instance.getPoints());
		
		for(int i=0; i<_instance.getPoints(); ++i)
			_neighbors.add(null);
	}
	
	public int[] get(int i)
	{
		if( _neighbors.get(i) == null )
			_neighbors.set(i, constructNeighbors(i));
		
		return _neighbors.get(i);
	}

	private int[] constructNeighbors(int i)
	{
		ArrayList<Integer> elems = new ArrayList<Integer>();
		for(int j=0; j<_instance.getPoints(); ++j) if( i != j )
			elems.add(j);
		
		Collections.sort(elems, (j,k) -> (int)Math.signum(_instance.getPoint(i).distance(_instance.getPoint(j)) - _instance.getPoint(i).distance(_instance.getPoint(k))));
		
		int[] ret = new int[_instance.getPoints()-1];
		for(int j=0; j<elems.size(); ++j)
			ret[j] = elems.get(j);
		
		return ret;
	}
}
