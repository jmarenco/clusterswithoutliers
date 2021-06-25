package clustering;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class Separator extends IloCplex.UserCutCallback
{
	private RectangularModel _model;
	private Instance _instance;
	private ArrayList<LinearSeparator> _linearSeparators;
	
	private static boolean _active = true;
	private static boolean _multithreaded = false;
	
	public Separator(RectangularModel model)
	{
		_model = model;
		_instance = model.getInstance();
		_linearSeparators = new ArrayList<LinearSeparator>();
		
		try
		{
			for(int i=0; i<_instance.getClusters(); ++i)
			for(int j=0; j<_instance.getDimension(); ++j)
				_linearSeparators.add(new LinearSeparator(this, i, j));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void main() throws IloException
	{
		if( this.isAfterCutLoop() == false || _active == false )
	        return;
		
		if( _multithreaded == false )
		{
			for(LinearSeparator linearSeparator: _linearSeparators)
				linearSeparator.separate();
		}
		else
		{
			try
			{
				ArrayList<LinearSeparatorThread> threads = new ArrayList<LinearSeparatorThread>(_linearSeparators.size());
				
				for(LinearSeparator linearSeparator: _linearSeparators)
					threads.add(new LinearSeparatorThread(linearSeparator));
				
				for(LinearSeparatorThread thread: threads)
					thread.start();
			
				for(LinearSeparatorThread thread: threads)
					thread.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public RectangularModel getRectangularModel()
	{
		return _model;
	}
	
	public double get(IloNumVar variable) throws IloException
	{
		return this.getValue(variable);
	}
	
	public void addCut(IloRange range, int cutManagement) throws IloException
	{
		this.add(range, cutManagement);
	}
}

