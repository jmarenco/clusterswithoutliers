package clustering;

public class Inequality
{
	private int _cluster;
	private int _dimension;

	private double _a;
	private double _b;
	private double _beta;
	private double[] _alpha;
	
	public Inequality(Instance instance, int cluster, int dimension)
	{
		_cluster = cluster;
		_dimension = dimension;
		_alpha = new double[instance.getPoints()];
	}
	
	public void setBeta(double beta)
	{
		_beta = beta;
	}

	public void setB(double b)
	{
		_b = b;
	}
	
	public void setA(double a)
	{
		_a = a;
	}
	
	public void setAlpha(int i, double alpha)
	{
		_alpha[i] = alpha;
	}
	
	public int getCluster()
	{
		return _cluster;
	}
	
	public int getDimension()
	{
		return _dimension;
	}
	
	public double getBeta()
	{
		return _beta;
	}

	public double getB()
	{
		return _b;
	}
	
	public double getA()
	{
		return _a;
	}
	
	public double getAlpha(int i)
	{
		return _alpha[i];
	}
}
