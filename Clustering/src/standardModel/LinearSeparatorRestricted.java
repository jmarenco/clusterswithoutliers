package standardModel;

import java.util.ArrayList;
import java.util.Collections;

import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class LinearSeparatorRestricted implements SeparatorInterface
{
	private Separator _parent;
	private RectangularModelInterface _model;
	private Instance _instance;
	
	private int _cluster;
	private int _dimension;
	private int[] _orderedIndices;
	
	private static double _threshold = 0.5;
	private static boolean _verbose = false;
	private static boolean _check = false;
	private static boolean _allClusters = false;
	
	private IloCplex cplex;
	private IloNumVar[] gamma;
	private IloNumVar delta;
	private IloNumVar alpha;
	private IloNumVar beta;
	private IloObjective objective;
	
	public LinearSeparatorRestricted(Separator parent, int cluster, int dimension) throws IloException
	{
		_parent = parent;
		_model = parent.getModelInterface();
		_instance = _model.getInstance();

		_cluster = cluster;
		_dimension = dimension;
		
		calculateOrderedIndices();
		createModel();
	}
	
	private void createModel() throws IloException
	{
		// Create model
		cplex = new IloCplex();
		cplex.setOut(null);
		cplex.setWarning(null);
		
		// Create variables
		gamma = new IloNumVar[_instance.getPoints()];
		delta = cplex.numVar(-1e10, 1e10, "d");
		alpha = cplex.numVar(0, 10 * _instance.getPoints(), "a");
		beta = cplex.numVar(0, 10 * _instance.getPoints(), "b");
		
		for(int i=0; i<_instance.getPoints(); ++i)
			gamma[i] = cplex.numVar(0, 1e10, "g" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, delta));
		fobj = cplex.sum(fobj, cplex.prod(-1.0, alpha));
		fobj = cplex.sum(fobj, cplex.prod(1.0, beta));

		for(int i=0; i<_instance.getPoints(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(1.0, gamma[i]));
		
		objective = cplex.addMaximize(fobj);

		// Create constraints for each point
		for(int t=0; t<_orderedIndices.length-1; ++t)
		{
			int i = _orderedIndices[t];
			int ip1 = _orderedIndices[t+1];
			
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(1.0, gamma[i]));
			lhs = cplex.sum(lhs, cplex.prod(-_instance.getPoint(ip1).get(_dimension), beta));
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(_dimension), beta));
			
			cplex.addLe(lhs, 0, "c" + i);
		}

		for(int i=0; i<_instance.getPoints(); ++i)
		{
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(1.0, gamma[i]));
			lhs = cplex.sum(lhs, cplex.prod(-1.0, delta));
			lhs = cplex.sum(lhs, cplex.prod(_instance.getPoint(i).get(_dimension), beta));
			lhs = cplex.sum(lhs, cplex.prod(-_instance.getPoint(i).get(_dimension), alpha));
			
			cplex.addLe(lhs, 0, "r" + i);
		}
		
		// Create normalization constraint
		IloNumExpr lhs3 = cplex.linearNumExpr();
		lhs3 = cplex.sum(lhs3, alpha);
		lhs3 = cplex.sum(lhs3, beta);

		cplex.addEq(lhs3, _instance.getPoints() + 1, "norm");
	}
	
	public void separate() throws IloException
	{
		// Update objective function
		cplex.setLinearCoef(objective, -rVar(_cluster, _dimension), alpha);
		cplex.setLinearCoef(objective, lVar(_cluster, _dimension), beta);

		for(int i=0; i<_instance.getPoints(); ++i)
			cplex.setLinearCoef(objective, zVar(i,_cluster), gamma[i]);

		// Solve
		cplex.solve();

		// The model should be feasible and bounded ...
		if( cplex.getStatus() != IloCplex.Status.Optimal )
		{
			System.err.println("LinearSeparator: " + cplex.getStatus());
			return;
		}
		
		if( _verbose == true )
			printInequality();
		
		if( _check == true )
			checkValidity();
		
		// If the inequality is violated, adds the inequality to the master problem
		if( violation() > _threshold )
		{
			if( _allClusters == false)
			{
				addCut(_cluster);
			}
			else
			{
				for(int i=0; i<_instance.getClusters(); ++i)
					addCut(i);
			}
		}
	}
	
	// Adds inequality to the master problem
	private void addCut(int cluster) throws IloException
	{
		IloCplex master = _model.getCplex();
		IloNumExpr inequality = master.linearNumExpr();
		
		inequality = master.sum(inequality, master.prod(cplex.getValue(alpha), _model.rVar(cluster, _dimension)));
		inequality = master.sum(inequality, master.prod(-cplex.getValue(beta), _model.lVar(cluster, _dimension)));
		
		for(int i=0; i<_instance.getPoints(); ++i)
			inequality = master.sum(inequality, master.prod(-cplex.getValue(gamma[i]), _model.zVar(i, cluster)));
				
		_parent.addCut( master.ge(inequality, -cplex.getValue(delta)), IloCplex.CutManagement.UseCutForce );
	}
	
	// Violation of the found inequality for the current point
	private double violation() throws IloException
	{
		double lhsval = cplex.getValue(alpha) * _parent.get(_model.rVar(_cluster, _dimension)) - cplex.getValue(beta) * _parent.get(_model.lVar(_cluster, _dimension));
		double rhsval = -cplex.getValue(delta);

		for(int i=0; i<_instance.getPoints(); ++i)
			rhsval += cplex.getValue(gamma[i]) * _parent.get(_model.zVar(i, _cluster));
		
		return rhsval-lhsval;
	}
	
	// Prints the inequality to the console
	private void printInequality() throws IloException
	{
		System.out.printf("%.2f * r", cplex.getValue(alpha));
		System.out.printf(" - %.2f * l >=", cplex.getValue(beta));
		
		for(int i=0; i<_instance.getPoints(); ++i) if( Math.abs(cplex.getValue(gamma[i])) > 0.001 )
			System.out.printf(" + %.2f * z[%d]", cplex.getValue(gamma[i]), i);

		System.out.printf(" - %.2f", cplex.getValue(delta));
		System.out.printf(" (viol: %.2f)", violation());
		System.out.println();
	}
	
	// Checks if the current inequality is valid 
	private void checkValidity() throws IloException
	{
		boolean[] x = new boolean[_instance.getPoints() + 1];
		x[0] = true;
		
		while( x[_instance.getPoints()] == false )
		{
			double min = _instance.max(_dimension);
			double max = _instance.min(_dimension);
			
			double lhs = 0;
			for(int i=0; i<_instance.getPoints(); ++i) if( x[i] == true )
			{
				min = Math.min(min, _instance.getPoint(i).get(_dimension));
				max = Math.max(max, _instance.getPoint(i).get(_dimension));
				lhs -= cplex.getValue(gamma[i]);
			}
			
			lhs += cplex.getValue(alpha) * max - cplex.getValue(beta) * min + cplex.getValue(delta);
			
			if (lhs < -0.01)
			{
				printInequality();
				
				for(int t=0; t<_orderedIndices.length; ++t)
					System.out.print(_orderedIndices[t] + " ");

				System.out.println();
				System.out.print("************** Not valid! a = ");
				
				for(int i=0; i<_instance.getPoints(); ++i)
					System.out.print(x[i] ? "1 " : "0 ");
				
				System.out.println("- dim: " + _dimension + ", min: " + min + ", max: " + max + ", lhs: " + lhs);
				System.out.println("Status: " + cplex.getStatus());
				System.exit(1);
			}

			int j = 0;
			while( x[j] == true )
			{
				x[j] = false;
				++j;
			}
			
			x[j] = true;
		}
	}
	
	// Closes the separator
	public void end()
	{
		if( cplex != null )
			cplex.end();
	}
	
	// Sort the poitns in the current dimension in ascending order
	public void calculateOrderedIndices()
	{
		ArrayList<Integer> indices = new ArrayList<Integer>();
		for(int i=0; i<_instance.getPoints(); ++i)
			indices.add(i);

		Collections.sort(indices, (i,j) -> (int)Math.signum(_instance.getPoint(i).get(_dimension) - _instance.getPoint(j).get(_dimension)));
		
		_orderedIndices = new int[_instance.getPoints()];
		for(int i=0; i<_instance.getPoints(); ++i)
			_orderedIndices[i] = indices.get(i);
	}

	// Current master solution
	public double zVar(int point, int cluster) throws IloException
	{
		return _parent.get(_model.zVar(point, cluster));
	}
	public double rVar(int cluster, int dimension) throws IloException
	{
		return _parent.get(_model.rVar(cluster, dimension));
	}
	public double lVar(int cluster, int dimension) throws IloException
	{
		return _parent.get(_model.lVar(cluster, dimension));
	}
	
	// Parameters
	public static void setThreshold(double value)
	{
		_threshold = value;
	}
	public static double getThreshold()
	{
		return _threshold;
	}
}

