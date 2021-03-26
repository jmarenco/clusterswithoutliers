package clustering;

import java.util.Random;

public class HeuristicGenerator
{
	private Instance _instance;
	private Master _master;
	private Random random = new Random(0);

	public HeuristicGenerator(Instance instance, Master master)
	{
		_instance = instance;
		_master = master;
	}
	
	public Cluster solve()
	{
		Cluster ret = null;
		for(int j=0; j<100; ++j)
		{
			Cluster current = new Cluster();
			
			for(int i=0; i<_instance.getPoints(); ++i) if( random.nextBoolean() )
				current.add(_instance.getPoint(i));
			
			if( ret == null || ret.reducedCost(_instance, _master) > current.reducedCost(_instance, _master) )
				ret = current;
		}
		
		return ret.reducedCost(_instance, _master) < -0.01 ? ret : null;
	}
}
