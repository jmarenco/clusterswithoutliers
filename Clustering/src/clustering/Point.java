package clustering;

import java.util.Arrays;

public class Point
{
	private int _id;
	private double[] _values;
	
	public Point(int id, int dimension)
	{
		_id = id;
		_values = new double[dimension];
	}
	
	public static Point fromVector(int id, double... values)
	{
		Point ret = new Point(id, values.length);
		
		for(int i=0; i<values.length; ++i)
			ret.set(i, values[i]);
		
		return ret;
	}
	
	public void setId(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getDimension()
	{
		return _values.length;
	}
	
	public void set(int i, double value)
	{
		if( i < 0 || i >= _values.length )
			throw new RuntimeException("Out of range Point coordinate: " + i);
			
		_values[i] = value;
	}
	
	public double get(int i)
	{
		if( i < 0 || i >= _values.length )
			throw new RuntimeException("Out of range Point coordinate: " + i);
		
		return _values[i];
	}
	
	public void sum(Point other)
	{
		if( this.getDimension() != other.getDimension() )
			throw new RuntimeException("Summing points of different dimension");
		
		for(int i=0; i<getDimension(); ++i)
			_values[i] += other.get(i);
	}

	public void divide(double factor)
	{
		if( factor == 0 )
			throw new RuntimeException("Dividing a point by zero");
		
		for(int i=0; i<getDimension(); ++i)
			_values[i] /= factor;
	}
	
	public double distance(Point other)
	{
		if( this.getDimension() != other.getDimension() )
			throw new RuntimeException("Taking distance betweemn points of different dimension");

		double sum = 0;
		for(int i=0; i<getDimension(); ++i)
			sum += (this.get(i) - other.get(i)) * (this.get(i) - other.get(i));
		
		return Math.sqrt(sum);
	}
	
	public Point clone()
	{
		return Point.fromVector(_id, _values);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + _id;
		result = prime * result + Arrays.hashCode(_values);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Point other = (Point) obj;
		if (_id != other._id)
			return false;
		if (!Arrays.equals(_values, other._values))
			return false;
		return true;
	}
}
