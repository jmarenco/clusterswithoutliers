package clustering;

public class RectangularCluster extends Cluster
{
	private double[] _min;
	private double[] _max;
	
	public RectangularCluster(int dimension)
	{
		super();
		
		_min = new double[dimension];
		_max = new double[dimension];
	}
	
	public void setMin(int coordinate, double value)
	{
		_min[coordinate] = value;
	}
	
	public void setMax(int coordinate, double value)
	{
		_max[coordinate] = value;
	}

	public double getMin(int coordinate)
	{
		return _min[coordinate];
	}
	
	public double getMax(int coordinate)
	{
		return _max[coordinate];
	}
	
	@Override
	public double objective()
	{
		double ret = 0;
		for(int t=0; t<_max.length; ++t)
			ret += _max[t] - _min[t];
		
		return ret;
	}
}
