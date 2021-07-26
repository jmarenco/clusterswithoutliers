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
	private static boolean _cutAndBranch = false;
	private static int _maxRounds = 10;
	
	private IloCplex.NodeId _root;
	private IloCplex.NodeId _lastNode;
	private int _rounds;

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
		
		IloCplex.NodeId current = updateNodes();

		if( _cutAndBranch && current.equals(_root) == false )
			return;
		
		if( _rounds++ > _maxRounds )
			return;
		
		for(LinearSeparator linearSeparator: _linearSeparators)
			linearSeparator.separate();
	}
	
	private IloCplex.NodeId updateNodes() throws IloException
	{
		IloCplex.NodeId current = this.getNodeId();
		
		if( current == null )
			return null;
		
		if( _root == null )
			_root = current;
		
		if( current.equals(_lastNode) == false )
		{
			_lastNode = current;
			_rounds = 0;
		}
		
		_rounds++;
		
		return current;
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

