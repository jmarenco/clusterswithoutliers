package general;

public class Clock 
{
	public Clock()
	{
		_running = false;
		_timelimit = -1;
	}

	public Clock(double timelimit)
	{
		_running = false;
		_timelimit = timelimit;
	}

	public void setTimelimit(double timelimit)
	{
		_timelimit = timelimit;
	}

	public void start()
	{
		_running = true;
		_start_time = System.currentTimeMillis();
	};

	public void start(double timelimit)
	{
		setTimelimit(timelimit);
		start();
	};

	public void stop()
	{
		_running = false;
		_end_time = System.currentTimeMillis();
	}

	public double elapsed()
	{
		long end;
		if (_running)
			end = System.currentTimeMillis();
		else
			end = _end_time;

		return (end - _start_time) / 1000.0;
	}

	public boolean timeout()
	{
		return _timelimit > 0 && elapsed() > _timelimit;
	}

	public double remaining()
	{
		double ret = 0;
		if (_timelimit > 0)
			ret = _timelimit - elapsed();

		return ret > 0 ? ret : 0;
	}

	public double getTimeLimit() 
	{
		return _timelimit;
	}

	private long _start_time, _end_time;
	private boolean _running;
	private double _timelimit;

}
