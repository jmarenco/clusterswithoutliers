package standardModel;

import java.util.ArrayList;

import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import standardModel.SquareSeparator.SparsingStrategy;

public class Separator extends IloCplex.UserCutCallback
{
	private RectangularModelInterface _model;
	private Instance _instance;
	private ArrayList<SeparatorInterface> _linearSeparators;
	
	private static boolean _active = true;
	private static boolean _cutAndBranch = false;
	private static int _maxRounds = 10;
	private static int _skipFactor = 0;
	private static int _strategy = 0;
	
	private IloCplex.NodeId _root;
	private IloCplex.NodeId _lastNode;
	private int _rounds;
	private int _skipped;
	private int _executions = 0;

	public Separator(RectangularModelInterface model)
	{
		_model = model;
		_instance = model.getInstance();
		_linearSeparators = new ArrayList<SeparatorInterface>();
		
		try
		{
			for(int i=0; i<_instance.getClusters(); ++i)
			for(int j=0; j<_instance.getDimension(); ++j)
			{
				if( _strategy == 0 )
					_linearSeparators.add(new LinearSeparator(this, i, j));

				if( _strategy == 1 )
					_linearSeparators.add(new LinearSeparatorSparse(this, i, j));

				if( _strategy == 2 )
					_linearSeparators.add(new LinearSeparatorRestricted(this, i, j));
			}
			
			if( _strategy == 3 || _strategy == 4 )
			{
				for(int i=0; i<_instance.getClusters(); ++i)
					_linearSeparators.add(new SquareSeparator(this, i, _strategy == 3 ? SparsingStrategy.None : SparsingStrategy.Random));
			}

			if( _strategy == 5 )
			{
				for(int i=0; i<_instance.getClusters(); ++i)
				{
					_linearSeparators.add(new SquareSeparator(this, i, SparsingStrategy.FirstQuadrant));
					_linearSeparators.add(new SquareSeparator(this, i, SparsingStrategy.SecondQuadrant));
					_linearSeparators.add(new SquareSeparator(this, i, SparsingStrategy.ThirdQuadrant));
					_linearSeparators.add(new SquareSeparator(this, i, SparsingStrategy.FourthQuadrant));
				}
			}
			
			if( _strategy == 6 )
			{
				for(int i=0; i<_instance.getClusters(); ++i)
					_linearSeparators.add(new SquareSeparatorSparse(this, i));

//				for(int i=0; i<_instance.getClusters(); ++i)
//				for(int j=0; j<_instance.getDimension(); ++j)
//					_linearSeparators.add(new LinearSeparator(this, i, j));
			}
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
		
		IloCplex.NodeId current = updateNodes();

		if( _cutAndBranch && current.equals(_root) == false )
			return;
		
		if( _rounds > _maxRounds )
			return;
		
		if (_skipped < _skipFactor )
			return;
		
		for(SeparatorInterface linearSeparator: _linearSeparators)
			linearSeparator.separate();
		
		_skipped = 0;
		_executions += 1;
	}
	
	private IloCplex.NodeId updateNodes() throws IloException
	{
		IloCplex.NodeId current = this.getNodeId();
		
		if( current == null )
			return null;
		
		if( _root == null )
		{
			_root = current;
			_skipped = _skipFactor; // Cuts in the first node
		}
		
		if( current.equals(_lastNode) == false )
		{
			_lastNode = current;
			_rounds = 0;
			_skipped += 1;
		}
		
		_rounds++;
		
		return current;
	}
	
	public RectangularModelInterface getModelInterface()
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
	
	public static void setActive(boolean active)
	{
		_active = active;
	}
	
	public static void setCutAndBranch(boolean cutAndBranch)
	{
		_cutAndBranch = cutAndBranch;
	}
	
	public static void setMaxRounds(int maxRounds)
	{
		_maxRounds = maxRounds;
	}
	
	public static void setSkipFactor(int skipFactor)
	{
		_skipFactor = skipFactor;
	}
	
	public static void setStrategy(int strategy)
	{
		_strategy = strategy;
	}
	
	public static boolean getCutAndBranch()
	{
		return _cutAndBranch;
	}

	public static int getMaxRounds()
	{
		return _maxRounds;
	}
	
	public static int getSkipFactor()
	{
		return _skipFactor;
	}
	
	public static int getStrategy()
	{
		return _strategy;
	}

	public int getExecutions()
	{
		return _executions;
	}
}
