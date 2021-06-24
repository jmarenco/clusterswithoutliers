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
	private boolean _active = true;
	
	public Separator(RectangularModel model)
	{
		_model = model;
		_instance = model.getInstance();
		_linearSeparators = new ArrayList<LinearSeparator>();
		
		for(int i=0; i<_instance.getClusters(); ++i)
		for(int j=0; j<_instance.getDimension(); ++j)
			_linearSeparators.add(new LinearSeparator(this, i, j));
	}
	
	@Override
	protected void main() throws IloException
	{
		if( this.isAfterCutLoop() == false || _active == false )
	        return;
		
		for(LinearSeparator linearSeparator: _linearSeparators)
			linearSeparator.separate();
	}
	
	public RectangularModel getRectangularModel()
	{
		return _model;
	}
	
	public double getValor(IloNumVar variable) throws IloException
	{
		return this.getValue(variable);
	}
	
	public void agregar(IloRange range, int cutManagement) throws IloException
	{
		this.add(range, cutManagement);
	}
}

