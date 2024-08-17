package incremental;

import java.util.ArrayList;

import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import standardModel.RectangularModelInterface;

public class LazySeparator extends IloCplex.LazyConstraintCallback
{
	private RectangularModelInterface _model;
	private Instance _instance;
	private ArrayList<LazySeparatorInterface> _lazySeparators;
	
	private int _executions = 0;

	public LazySeparator(RectangularModelInterface model)
	{
		_model = model;
		_instance = model.getInstance();
		_lazySeparators = new ArrayList<LazySeparatorInterface>();
		
		try
		{
			if (IncrementalSolver.incrementalMetric == IncrementalSolver.Metric.None)
				_lazySeparators.add(new LazyDummySeparator());
			else
				_lazySeparators.add(new LazyCoveringSeparator(this, _instance));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	protected void main() throws IloException
	{
		for(LazySeparatorInterface lazySeparator: _lazySeparators)
			lazySeparator.separate();
		
		_executions += 1;
		_model.getCplex().exportModel("model_" + _executions + ".lp");
	}
	
	public RectangularModelInterface getModelInterface()
	{
		return _model;
	}
	
	public double get(IloNumVar variable) throws IloException
	{
		return this.getValue(variable);
	}
	
	public void addCut(IloRange range) throws IloException
	{
		this.add(range, IloCplex.CutManagement.UseCutForce );
	}
	
	public void addCut(IloRange range, int cutManagement) throws IloException
	{
		this.add(range, cutManagement);
	}
	
	// Helpers
	public double zVar(int point, int cluster) throws IloException
	{
		return get(_model.zVar(point, cluster));
	}
	public double rVar(int cluster, int dimension) throws IloException
	{
		return get(_model.rVar(cluster, dimension));
	}
	public double lVar(int cluster, int dimension) throws IloException
	{
		return get(_model.lVar(cluster, dimension));
	}
	
	public int getExecutions()
	{
		return _executions;
	}

}
