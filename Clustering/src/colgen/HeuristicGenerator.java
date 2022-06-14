package colgen;

import java.util.Random;

import general.Cluster;
import general.Instance;

public class HeuristicGenerator
{
	private Instance _instance;
	private MasterPacking _master;
	private Random random = new Random(0);

	public HeuristicGenerator(Instance instance, MasterPacking master)
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
			
			if( ret == null || _master.reducedCost(ret) > _master.reducedCost(current) )
				ret = current;
		}
		
		return _master.reducedCost(ret) < -0.01 ? ret : null;
	}
}
