package clustering;

public class LinearSeparatorThread extends Thread
{
	private LinearSeparator _linearSeparator;

	public LinearSeparatorThread(LinearSeparator linearSeparator)
	{
		_linearSeparator = linearSeparator;
	}
	
	@Override public void run()
	{
		try
		{
			_linearSeparator.separate();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
