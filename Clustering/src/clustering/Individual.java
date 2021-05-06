package clustering;

import java.util.Random;

public class Individual implements Comparable<Individual>
{
	private boolean[] _bits;

	private Instance _instance;
	private MasterPacking _master;
	
	private static Random _random = new Random(0);
	
	public static Individual random(Instance instance, MasterPacking master)
	{
		Individual ret = new Individual(instance, master);
		
		for(int i=0; i<instance.getPoints(); ++i)
			ret.set(i, _random.nextBoolean());
		
		return ret;
	}

	private Individual(Instance instance, MasterPacking master)
	{
		_instance = instance;
		_master = master;
		_bits = new boolean[instance.getPoints()];
	}

	public void mutate()
	{
		int i = _random.nextInt(_bits.length);
		_bits[i] = !_bits[i];
	}
	
	public Individual[] recombine(Individual that)
	{
		int k = _random.nextInt(_bits.length);
		
		Individual son1 = new Individual(_instance, _master);
		Individual son2 = new Individual(_instance, _master);
		
		for(int i=0; i<k; ++i)
		{
			son1.set(i, this.get(i));
			son2.set(i, that.get(i));
		}
		
		for(int i=k; i<_bits.length; ++i)
		{
			son1.set(i, that.get(i));
			son2.set(i, this.get(i));
		}
		
		return new Individual[] { son1, son2 };
	}
	
	public Cluster asCluster()
	{
		Cluster ret = new Cluster();
		for(int i=0; i<_instance.getPoints(); ++i) if( get(i) == true )
			ret.add(_instance.getPoint(i));
		
		return ret;
	}
	
	public double fitness()
	{
		return -_master.reducedCost(asCluster());
	}
	
	boolean get(int i)
	{
		return _bits[i];
	}
	private void set(int i, boolean value)
	{
		_bits[i] = value;
	}

	@Override
	public int compareTo(Individual other)
	{
		if( this.fitness() < other.fitness() )
			return -1;
		else if( this.fitness() == other.fitness() )
			return 0;
		else
			return 1;
	}
}