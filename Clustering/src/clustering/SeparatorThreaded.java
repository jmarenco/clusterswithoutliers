package clustering;

import java.util.ArrayList;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

@Deprecated
public class SeparatorThreaded extends IloCplex.UserCutCallback
{
	private RectangularModel _model;
	private Instance _instance;
	private ArrayList<LinearSeparatorThreaded> _threadedSeparators;
	
	private static boolean _active = true;
	
	public SeparatorThreaded(RectangularModel model)
	{
		_model = model;
		_instance = model.getInstance();
		_threadedSeparators = new ArrayList<LinearSeparatorThreaded>();
		
		try
		{
			for(int i=0; i<_instance.getClusters(); ++i)
			for(int j=0; j<_instance.getDimension(); ++j)
				_threadedSeparators.add(new LinearSeparatorThreaded(this, i, j));
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
		
		try
		{
			ArrayList<LinearSeparatorThreaded.LinearSeparatorThread> threads = new ArrayList<LinearSeparatorThreaded.LinearSeparatorThread>(_threadedSeparators.size());
				
			for(LinearSeparatorThreaded linearSeparator: _threadedSeparators)
				threads.add(new LinearSeparatorThreaded.LinearSeparatorThread(linearSeparator));
				
			for(LinearSeparatorThreaded.LinearSeparatorThread thread: threads)
				thread.start();
			
			for(LinearSeparatorThreaded.LinearSeparatorThread thread: threads)
				thread.join();
				
			for(LinearSeparatorThreaded.LinearSeparatorThread thread: threads) if( thread.getInequality() != null )
				addCut(thread.getInequality());
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
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
	
	public void addCut(Inequality inequality) throws IloException
	{
		IloCplex master = _model.getCplex();
		IloNumExpr lhs = master.linearNumExpr();
		
		lhs = master.sum(lhs, master.prod(inequality.getA(), _model.rVar(inequality.getCluster(), inequality.getDimension())));
		lhs = master.sum(lhs, master.prod(-inequality.getB(), _model.lVar(inequality.getCluster(), inequality.getDimension())));
		
		for(int i=0; i<_instance.getPoints(); ++i)
			lhs = master.sum(lhs, master.prod(-inequality.getAlpha(i), _model.zVar(i, inequality.getCluster())));
				
		this.add( master.ge(lhs, -inequality.getBeta()), IloCplex.CutManagement.UseCutForce );
	}
}

