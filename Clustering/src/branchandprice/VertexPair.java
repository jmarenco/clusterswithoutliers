package branchandprice;

public class VertexPair<T>
{
	private T _a;
	private T _b;
	
	public VertexPair(T a, T b)
	{
		_a = a;
		_b = b;
	}
	
	public T getFirst()
	{
		return _a;
	}
	
	public T getSecond()
	{
		return _b;
	}
}