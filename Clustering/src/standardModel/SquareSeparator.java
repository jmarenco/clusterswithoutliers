package standardModel;

import java.util.ArrayList;
import java.util.Collections;

import general.Instance;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class SquareSeparator implements SeparatorInterface
{
	private Separator _parent;
	private RectangularModel _model;
	private Instance _instance;
	
	private int _cluster;
	private int _dimension1;
	private int _dimension2;
	private double[] _coordinates1;
	private double[] _coordinates2;
	
	private static double _threshold = 0.5;
	private static boolean _verbose = false;
	private static boolean _check = false;
	private static boolean _allClusters = false;
	
	private IloCplex cplex;
	private IloNumVar[] alpha;
	private IloNumVar beta;
	private IloNumVar a1;
	private IloNumVar b1;
	private IloNumVar a2;
	private IloNumVar b2;
	private IloObjective objective;
	
	public SquareSeparator(Separator parent, int cluster) throws IloException
	{
		_parent = parent;
		_model = parent.getRectangularModel();
		_instance = _model.getInstance();

		_cluster = cluster;
		_dimension1 = 0;
		_dimension2 = 1;
		_coordinates1 = getCoordinates(0);
		_coordinates2 = getCoordinates(1);
		
		createModel();
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
		a1 = cplex.numVar(0, 10 * _instance.getPoints(), "a1");
		b1 = cplex.numVar(0, 10 * _instance.getPoints(), "b1");
		a2 = cplex.numVar(0, 10 * _instance.getPoints(), "a2");
		b2 = cplex.numVar(0, 10 * _instance.getPoints(), "b2");
		
		for(int i=0; i<_instance.getPoints(); ++i)
			alpha[i] = cplex.numVar(0, 1e10, "alfa" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, beta));
		fobj = cplex.sum(fobj, cplex.prod(-1.0, a1));
		fobj = cplex.sum(fobj, cplex.prod(1.0, b1));
		fobj = cplex.sum(fobj, cplex.prod(-1.0, a2));
		fobj = cplex.sum(fobj, cplex.prod(1.0, b2));

		for(int i=0; i<_instance.getPoints(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(1.0, alpha[i]));
		
		objective = cplex.addMaximize(fobj);

		// Create constraints for pairs of points
		for(int i1=0; i1<_coordinates1.length; ++i1)
		for(int j1=i1; j1<_coordinates1.length; ++j1)
		for(int i2=0; i2<_coordinates2.length; ++i2)
		for(int j2=i2; j2<_coordinates2.length; ++j2)
		{
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(-1.0, beta));
			lhs = cplex.sum(lhs, cplex.prod(-_coordinates1[j1], a1));
			lhs = cplex.sum(lhs, cplex.prod(_coordinates1[i1], b1));
			lhs = cplex.sum(lhs, cplex.prod(-_coordinates2[j2], a2));
			lhs = cplex.sum(lhs, cplex.prod(_coordinates2[i2], b2));
			
			for(int k=0; k<_instance.getPoints(); ++k) if( _coordinates1[i1]-0.001 <= _instance.getPoint(k).get(_dimension1) && _instance.getPoint(k).get(_dimension1) <= _coordinates1[j1]+0.001 && _coordinates2[i2]-0.001 <= _instance.getPoint(k).get(_dimension2) && _instance.getPoint(k).get(_dimension2) <= _coordinates2[j2]+0.001 )
				lhs = cplex.sum(lhs, alpha[k]);
			
			cplex.addLe(lhs, 0, "c" + i1 + "_" + j1 + "_" + i2 + "_" + j2);
		}

		// Create constraints for empty intervals
		IloNumExpr lhs1 = cplex.linearNumExpr();
		IloNumExpr lhs2 = cplex.linearNumExpr();
		IloNumExpr lhs3 = cplex.linearNumExpr();
		IloNumExpr lhs4 = cplex.linearNumExpr();

		lhs1 = cplex.sum(lhs1, cplex.prod(-1, beta));
		lhs1 = cplex.sum(lhs1, cplex.prod(-_instance.max(_dimension1), a1));
		lhs1 = cplex.sum(lhs1, cplex.prod(_instance.max(_dimension1), b1));
		
		lhs2 = cplex.sum(lhs2, cplex.prod(-1,  beta));
		lhs2 = cplex.sum(lhs2, cplex.prod(-_instance.min(_dimension1), a1));
		lhs2 = cplex.sum(lhs2, cplex.prod(_instance.min(_dimension1), b1));

		lhs3 = cplex.sum(lhs3, cplex.prod(-1, beta));
		lhs3 = cplex.sum(lhs3, cplex.prod(-_instance.max(_dimension2), a2));
		lhs3 = cplex.sum(lhs3, cplex.prod(_instance.max(_dimension2), b2));
		
		lhs4 = cplex.sum(lhs4, cplex.prod(-1,  beta));
		lhs4 = cplex.sum(lhs4, cplex.prod(-_instance.min(_dimension2), a2));
		lhs4 = cplex.sum(lhs4, cplex.prod(_instance.min(_dimension2), b2));
		
		cplex.addLe(lhs1, 0, "inflim1");
		cplex.addLe(lhs2, 0, "suplim1");
		cplex.addLe(lhs3, 0, "inflim2");
		cplex.addLe(lhs4, 0, "suplim2");
		
		// Create normalization constraint
		IloNumExpr lhs5 = cplex.linearNumExpr();
		lhs5 = cplex.sum(lhs5, a1);
		lhs5 = cplex.sum(lhs5, b1);
		lhs5 = cplex.sum(lhs5, a2);
		lhs5 = cplex.sum(lhs5, b2);

		cplex.addEq(lhs5, _instance.getPoints() + 1, "norm");
	}
	
	public void separate() throws IloException
	{
		// Update objective function
		cplex.setLinearCoef(objective, -rVar(_cluster, _dimension1), a1);
		cplex.setLinearCoef(objective, lVar(_cluster, _dimension1), b1);
		cplex.setLinearCoef(objective, -rVar(_cluster, _dimension2), a2);
		cplex.setLinearCoef(objective, lVar(_cluster, _dimension2), b2);

		for(int i=0; i<_instance.getPoints(); ++i)
			cplex.setLinearCoef(objective, zVar(i,_cluster), alpha[i]);

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
		
		inequality = master.sum(inequality, master.prod(cplex.getValue(a1), _model.rVar(cluster, _dimension1)));
		inequality = master.sum(inequality, master.prod(-cplex.getValue(b1), _model.lVar(cluster, _dimension1)));
		inequality = master.sum(inequality, master.prod(cplex.getValue(a2), _model.rVar(cluster, _dimension2)));
		inequality = master.sum(inequality, master.prod(-cplex.getValue(b2), _model.lVar(cluster, _dimension2)));
		
		for(int i=0; i<_instance.getPoints(); ++i)
			inequality = master.sum(inequality, master.prod(-cplex.getValue(alpha[i]), _model.zVar(i, cluster)));
				
		_parent.addCut( master.ge(inequality, -cplex.getValue(beta)), IloCplex.CutManagement.UseCutForce );
	}
	
	// Violation of the found inequality for the current point
	private double violation() throws IloException
	{
		double lhsval = cplex.getValue(a1) * _parent.get(_model.rVar(_cluster, _dimension1)) - cplex.getValue(b1) * _parent.get(_model.lVar(_cluster, _dimension1))
		              + cplex.getValue(a2) * _parent.get(_model.rVar(_cluster, _dimension2)) - cplex.getValue(b2) * _parent.get(_model.lVar(_cluster, _dimension2));

		double rhsval = -cplex.getValue(beta);

		for(int i=0; i<_instance.getPoints(); ++i)
			rhsval += cplex.getValue(alpha[i]) * _parent.get(_model.zVar(i, _cluster));
		
		return rhsval-lhsval;
	}
	
	// Prints the inequality to the console
	private void printInequality() throws IloException
	{
		System.out.printf("%.2f * r1", cplex.getValue(a1));
		System.out.printf(" - %.2f * l1 >=", cplex.getValue(b1));
		System.out.printf(" + %.2f * r2", cplex.getValue(a2));
		System.out.printf(" - %.2f * l2 >=", cplex.getValue(b2));
		
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
			double min1 = _instance.max(_dimension1);
			double max1 = _instance.min(_dimension1);
			double min2 = _instance.max(_dimension2);
			double max2 = _instance.min(_dimension2);
			
			double lhs = 0;
			for(int i=0; i<_instance.getPoints(); ++i) if( x[i] == true )
			{
				min1 = Math.min(min1, _instance.getPoint(i).get(_dimension1));
				max1 = Math.max(max1, _instance.getPoint(i).get(_dimension1));
				min2 = Math.min(min2, _instance.getPoint(i).get(_dimension2));
				max2 = Math.max(max2, _instance.getPoint(i).get(_dimension2));
				lhs -= cplex.getValue(alpha[i]);
			}
			
			lhs += cplex.getValue(a1) * max1 - cplex.getValue(b1) * min1 + cplex.getValue(a2) * max2 - cplex.getValue(b2) * min2 + cplex.getValue(beta);
			
			if (lhs < -0.01)
			{
				System.out.print("************** Not valid! a = ");
				
				for(int i=0; i<_instance.getPoints(); ++i)
					System.out.print(x[i] ? "1 " : "0 ");
				
				System.out.println("- dim: " + _dimension1 + ", min1: " + min1 + ", max1: " + max1 + ", min2: " + min2 + ", max2: " + max2 + ", lhs: " + lhs);
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
	
	// Gets the coordinates in current dimension, with no repetitions and in ascending order
	public double[] getCoordinates(int dimension)
	{
		ArrayList<Double> coordinates = new ArrayList<Double>();
		for(int i=0; i<_instance.getPoints(); ++i) if( !coordinates.contains(_instance.getPoint(i).get(dimension)))
			coordinates.add(_instance.getPoint(i).get(dimension));
		
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

