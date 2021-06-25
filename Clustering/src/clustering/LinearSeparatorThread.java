package clustering;

public class LinearSeparatorThread extends Thread
{
	private LinearSeparator _linearSeparator;
	private Inequality _inequality;

	public LinearSeparatorThread(LinearSeparator linearSeparator)
	{
		_linearSeparator = linearSeparator;
	}
	
	@Override public void run()
	{
		try
		{
			_inequality = _linearSeparator.separate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public Inequality getInequality()
	{
		return _inequality;
	}
}
