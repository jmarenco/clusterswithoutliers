package clustering;

import java.util.Random;

public class HeuristicGenerator
{
	private Instance _instance;
	private double[] _dual;
	private Random random = new Random(0);

	public HeuristicGenerator(Instance instance, Master master)
	{
		_instance = instance;
		_dual = new double[_instance.getPoints()+2];
		
		for(int i=0; i<_instance.getPoints()+2; ++i)
			_dual[i] = master.getDual(i);
	}
	
	public Cluster solve()
	{
		Cluster ret = null;
		for(int j=0; j<100; ++j)
		{
			Cluster current = new Cluster();
			for(int i=0; i<_instance.getPoints(); ++i) if( random.nextBoolean() )
				current.add(_instance.getPoint(i));
			
			if( ret == null || reducedCost(ret) > reducedCost(current) )
				ret = current;
		}
		
		return reducedCost(ret) < -0.01 ? ret : null;
	}
	
	public double reducedCost(Cluster cluster)
	{
		double ret = cluster.objective() - _dual[_instance.getPoints()] - cluster.size() * _dual[_instance.getPoints()+1];
		
		for(int i=0; i<_instance.getPoints(); ++i) if( cluster.contains(_instance.getPoint(i)) )
			ret -= _dual[i];

		return ret;
	}
}
