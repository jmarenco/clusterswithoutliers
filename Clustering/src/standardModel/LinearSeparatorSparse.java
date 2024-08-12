package standardModel;

import java.util.ArrayList;
import java.util.Collections;

import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class LinearSeparatorSparse implements SeparatorInterface
{
	private Separator _parent;
	private RectangularModelInterface _model;
	private Instance _instance;
	
	private int _cluster;
	private int _dimension;
	
	private static double _threshold = 0.5;
	private static double _lowerLimit = 0.1; // In order to consider a z-variable in the separation
	private static double _upperLimit = 0.9; // In order to consider a z-variable in the separation
	private static boolean _verbose = false;
	private static boolean _check = false;
	private static boolean _allClusters = false;
	
	private IloCplex cplex;
	private IloNumVar[] alpha;
	private IloNumVar beta;
	private IloNumVar a;
	private IloNumVar b;
	
	public LinearSeparatorSparse(Separator parent, int cluster, int dimension) throws IloException
	{
		_parent = parent;
		_model = parent.getModelInterface();
		_instance = _model.getInstance();

		_cluster = cluster;
		_dimension = dimension;
	}
	
	public void separate() throws IloException
	{
		// Create model
		createModel();

		// Solve
		cplex.solve();

		// The model should be feasible and bounded ...
		if( cplex.getStatus() != IloCplex.Status.Optimal )
		{
			cplex.end();
			
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

		cplex.end();
	}
	
	private void createModel() throws IloException
	{
		// Create model
		cplex = new IloCplex();
		cplex.setOut(null);
		cplex.setWarning(null);
		
		// Create variables
		alpha = new IloNumVar[_instance.getPoints()];
		beta = cplex.numVar(-1e10, 1e10, "beta");
		a = cplex.numVar(0, 10 * _instance.getPoints(), "a");
		b = cplex.numVar(0, 10 * _instance.getPoints(), "b");
		
		for(int i=0; i<_instance.getPoints(); ++i) if( zVar(i,_cluster) >= _lowerLimit && zVar(i,_cluster) <= _upperLimit )
			alpha[i] = cplex.numVar(0, 1e10, "alfa" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, beta));
		fobj = cplex.sum(fobj, cplex.prod(-rVar(_cluster, _dimension), a));
		fobj = cplex.sum(fobj, cplex.prod(lVar(_cluster, _dimension), b));

		for(int i=0; i<_instance.getPoints(); ++i) if( alpha[i] != null )
			fobj = cplex.sum(fobj, cplex.prod(zVar(i,_cluster), alpha[i]));
		
		cplex.addMaximize(fobj);

		// Create constraints for pairs of points
		double[] coordinates = getCoordinates();
		for(int i=0; i<coordinates.length; ++i)
		for(int j=i; j<coordinates.length; ++j)
		{
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(-1.0, beta));
			lhs = cplex.sum(lhs, cplex.prod(-coordinates[j], a));
			lhs = cplex.sum(lhs, cplex.prod(coordinates[i], b));
			
			for(int k=0; k<_instance.getPoints(); ++k) if( alpha[k] != null && coordinates[i]-0.001 <= _instance.getPoint(k).get(_dimension) && _instance.getPoint(k).get(_dimension) <= coordinates[j]+0.001)
				lhs = cplex.sum(lhs, alpha[k]);
			
			cplex.addLe(lhs, 0, "c" + i + "_" + j);
		}

		// Create constraints for empty intervals
		IloNumExpr lhs1 = cplex.linearNumExpr();
		IloNumExpr lhs2 = cplex.linearNumExpr();

		lhs1 = cplex.sum(lhs1, cplex.prod(-1, beta));
		lhs1 = cplex.sum(lhs1, cplex.prod(-_instance.max(_dimension), a));
		lhs1 = cplex.sum(lhs1, cplex.prod(_instance.max(_dimension), b));
		
		lhs2 = cplex.sum(lhs2, cplex.prod(-1,  beta));
		lhs2 = cplex.sum(lhs2, cplex.prod(-_instance.min(_dimension), a));
		lhs2 = cplex.sum(lhs2, cplex.prod(_instance.min(_dimension), b));
		
		cplex.addLe(lhs1, 0, "inflim");
		cplex.addLe(lhs2, 0, "suplim");
		
		// Create normalization constraint
		IloNumExpr lhs3 = cplex.linearNumExpr();
		lhs3 = cplex.sum(lhs3, a);
		lhs3 = cplex.sum(lhs3, b);

		cplex.addEq(lhs3, _instance.getPoints() + 1, "norm");
	}	
	
	// Gets the coordinates in current dimension for active variables, with no repetitions and in ascending order
	private double[] getCoordinates()
	{
		ArrayList<Double> coordinates = new ArrayList<Double>();
		for(int i=0; i<_instance.getPoints(); ++i) if( alpha[i] != null && !coordinates.contains(_instance.getPoint(i).get(_dimension)))
			coordinates.add(_instance.getPoint(i).get(_dimension));
		
		Collections.sort(coordinates);
		
		double[] ret = new double[coordinates.size()];
		for(int i=0; i<coordinates.size(); ++i)
			ret[i] = coordinates.get(i);
		
		return ret;
	}
	
	// Adds inequality to the master problem
	private void addCut(int cluster) throws IloException
	{
		IloCplex master = _model.getCplex();
		IloNumExpr inequality = master.linearNumExpr();
		
		inequality = master.sum(inequality, master.prod(cplex.getValue(a), _model.rVar(cluster, _dimension)));
		inequality = master.sum(inequality, master.prod(-cplex.getValue(b), _model.lVar(cluster, _dimension)));
		
		for(int i=0; i<_instance.getPoints(); ++i) if( alpha[i] != null )
			inequality = master.sum(inequality, master.prod(-cplex.getValue(alpha[i]), _model.zVar(i, cluster)));
				
		_parent.addCut( master.ge(inequality, -cplex.getValue(beta)), IloCplex.CutManagement.UseCutForce );
	}
	
	// Violation of the found inequality for the current point
	private double violation() throws IloException
	{
		double lhsval = cplex.getValue(a) * _parent.get(_model.rVar(_cluster, _dimension)) - cplex.getValue(b) * _parent.get(_model.lVar(_cluster, _dimension));
		double rhsval = -cplex.getValue(beta);

		for(int i=0; i<_instance.getPoints(); ++i) if( alpha[i] != null )
			rhsval += cplex.getValue(alpha[i]) * _parent.get(_model.zVar(i, _cluster));
		
		return rhsval-lhsval;
	}
	
	// Prints the inequality to the console
	private void printInequality() throws IloException
	{
		System.out.printf("%.2f * r", cplex.getValue(a));
		System.out.printf(" - %.2f * l >=", cplex.getValue(b));
		
		for(int i=0; i<_instance.getPoints(); ++i) if( alpha[i] != null && Math.abs(cplex.getValue(alpha[i])) > 0.001 )
			System.out.printf(" + %.2f * z[%d]", cplex.getValue(alpha[i]), i);

		System.out.printf(" - %.2f", cplex.getValue(beta));
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
				lhs -= alpha[i] != null ? cplex.getValue(alpha[i]) : 0;
			}
			
			lhs += cplex.getValue(a) * max - cplex.getValue(b) * min + cplex.getValue(beta);
			
			if (lhs < -0.01)
			{
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
	public static void setLowerLimit(double value)
	{
		_lowerLimit = value;
	}
	public static void setUpperLimit(double value)
	{
		_upperLimit = value;
	}
	public static double getLowerLimit(double value)
	{
		return _lowerLimit;
	}
	public static double getUpperLimit(double value)
	{
		return _upperLimit;
	}
}

