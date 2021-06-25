package clustering;

import java.util.ArrayList;
import java.util.Collections;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

@Deprecated
public class LinearSeparatorThreaded
{
	private SeparatorThreaded _parent;
	private RectangularModel _model;
	private Instance _instance;
	
	private int _cluster;
	private int _dimension;
	private double[] _coordinates;
	
	private static double _threshold = 0.5;
	private static boolean _verbose = false;
	private static boolean _check = false;
	
	private IloCplex cplex;
	private IloNumVar[] alpha;
	private IloNumVar beta;
	private IloNumVar a;
	private IloNumVar b;
	private IloObjective objective;
	
	public LinearSeparatorThreaded(SeparatorThreaded parent, int cluster, int dimension) throws IloException
	{
		_parent = parent;
		_model = parent.getRectangularModel();
		_instance = _model.getInstance();

		_cluster = cluster;
		_dimension = dimension;
		_coordinates = getCoordinates();
		
		createModel();
	}
	
	private void createModel() throws IloException
	{
		// Create model
		cplex = new IloCplex();
		cplex.setOut(null);
		
		// Create variables
		alpha = new IloNumVar[_instance.getPoints()];
		beta = cplex.numVar(-1e10, 1e10, "beta");
		a = cplex.numVar(0, 10 * _instance.getPoints(), "a");
		b = cplex.numVar(0, 10 * _instance.getPoints(), "b");
		
		for(int i=0; i<_instance.getPoints(); ++i)
			alpha[i] = cplex.numVar(0, 1e10, "alfa" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, beta));
		fobj = cplex.sum(fobj, cplex.prod(-1.0, a));
		fobj = cplex.sum(fobj, cplex.prod(1-0, b));

		for(int i=0; i<_instance.getPoints(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(1.0, alpha[i]));
		
		objective = cplex.addMaximize(fobj);

		// Create constraints for pairs of points
		for(int i=0; i<_coordinates.length; ++i)
		for(int j=i; j<_coordinates.length; ++j)
		{
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(-1.0, beta));
			lhs = cplex.sum(lhs, cplex.prod(-_coordinates[j], a));
			lhs = cplex.sum(lhs, cplex.prod(_coordinates[i], b));
			
			for(int k=0; k<_instance.getPoints(); ++k) if( _coordinates[i]-0.001 <= _instance.getPoint(k).get(_dimension) && _instance.getPoint(k).get(_dimension) <= _coordinates[j]+0.001)
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
	
	public Inequality separate() throws IloException
	{
		// Update objective function
		cplex.setLinearCoef(objective, -rVar(_cluster, _dimension), a);
		cplex.setLinearCoef(objective, lVar(_cluster, _dimension), b);

		for(int i=0; i<_instance.getPoints(); ++i)
			cplex.setLinearCoef(objective, zVar(i,_cluster), alpha[i]);

		// Solve
		cplex.solve();

		// The model should be feasible and bounded ...
		if( cplex.getStatus() != IloCplex.Status.Optimal )
		{
			System.err.println("LinearSeparator: " + cplex.getStatus());
			return null;
		}
		
		if( _verbose == true )
			printInequality();
		
		if( _check == true )
			checkValidity();
		
		// If the inequality is violated, keeps the inequality
		if( violation() > _threshold )
		{
			Inequality ret = new Inequality(_instance, _cluster, _dimension);
			ret.setA(cplex.getValue(a));
			ret.setB(cplex.getValue(b));
			ret.setBeta(cplex.getValue(beta));
			
			for(int i=0; i<_instance.getPoints(); ++i)
				ret.setAlpha(i, cplex.getValue(alpha[i]));
			
			return ret;
		}
		
		return null;
	}
	
	// Violation of the found inequality for the current point
	private double violation() throws IloException
	{
		double lhsval = cplex.getValue(a) * _parent.get(_model.rVar(_cluster, _dimension)) - cplex.getValue(b) * _parent.get(_model.lVar(_cluster, _dimension));
		double rhsval = -cplex.getValue(beta);

		for(int i=0; i<_instance.getPoints(); ++i)
			rhsval += cplex.getValue(alpha[i]) * _parent.get(_model.zVar(i, _cluster));
		
		return rhsval-lhsval;
	}
	
	// Prints the inequality to the console
	private void printInequality() throws IloException
	{
		System.out.printf("%.2f * r", cplex.getValue(a));
		System.out.printf(" - %.2f * l >=", cplex.getValue(b));
		
		for(int i=0; i<_instance.getPoints(); ++i) if( Math.abs(cplex.getValue(alpha[i])) > 0.001 )
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
				lhs -= cplex.getValue(alpha[i]);
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
	
	public void end()
	{
		if( cplex != null )
			cplex.end();
	}
	
	// Gets the coordinates in current dimension, with no repetitions and in ascending order
	public double[] getCoordinates()
	{
		ArrayList<Double> coordinates = new ArrayList<Double>();
		for(int i=0; i<_instance.getPoints(); ++i) if( !coordinates.contains(_instance.getPoint(i).get(_dimension)))
			coordinates.add(_instance.getPoint(i).get(_dimension));
		
		Collections.sort(coordinates);
		
		double[] ret = new double[coordinates.size()];
		for(int i=0; i<coordinates.size(); ++i)
			ret[i] = coordinates.get(i);
		
		return ret;
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
	
	public static class LinearSeparatorThread extends Thread
	{
		private LinearSeparatorThreaded _linearSeparator;
		private Inequality _inequality;

		public LinearSeparatorThread(LinearSeparatorThreaded linearSeparator)
		{
			_linearSeparator = linearSeparator;
		}
		
		@Override public void run()
		{
			try
			{
				_inequality = _linearSeparator.separate();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		public Inequality getInequality()
		{
			return _inequality;
		}
	}
	
}

