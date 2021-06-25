package clustering;

import java.util.ArrayList;
import java.util.Collections;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

@Deprecated
public class StandaloneLinearSeparator
{
	private Separator _parent;
	private RectangularModel _model;
	private Instance _instance;
	
	private int _cluster;
	private int _dimension;
	private double[] _coordinates;
	private boolean _silent = true;
	private boolean _check = false;
	
	public StandaloneLinearSeparator(Separator parent, int cluster, int dimension)
	{
		_parent = parent;
		_model = parent.getRectangularModel();
		_instance = _model.getInstance();

		_cluster = cluster;
		_dimension = dimension;
		_coordinates = getCoordinates();
	}
	
	public void separate() throws IloException
	{
		// Create model and variables
		IloCplex cplex = new IloCplex();
		IloNumVar[] alpha = new IloNumVar[_instance.getPoints()];
		IloNumVar beta = cplex.numVar(-1e10, 1e10, "beta");
		IloNumVar a = cplex.numVar(0, 10 * _instance.getPoints(), "a");
		IloNumVar b = cplex.numVar(0, 10 * _instance.getPoints(), "b");
		
		for(int i=0; i<_instance.getPoints(); ++i)
			alpha[i] = cplex.numVar(0, 1e10, "alfa" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, beta));
		fobj = cplex.sum(fobj, cplex.prod(-rVar(_cluster, _dimension), a));
		fobj = cplex.sum(fobj, cplex.prod(lVar(_cluster, _dimension), b));

		for(int i=0; i<_instance.getPoints(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(zVar(i,_cluster), alpha[i]));
		
		cplex.addMaximize(fobj);

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
//		lhs3 = cplex.sum(lhs3, cplex.prod(-1.0, beta));
		lhs3 = cplex.sum(lhs3, a);
		lhs3 = cplex.sum(lhs3, b);

//		for(int i=0; i<_instance.getPoints(); ++i)
//			lhs3 = cplex.sum(lhs3, alpha[i]);
		
		cplex.addEq(lhs3, _instance.getPoints() + 1, "norm");
		
		// Solve
//		cplex.exportModel("separator.lp");
		cplex.setOut(null);
		cplex.solve();

		if( cplex.getStatus() != IloCplex.Status.Optimal )
		{
			System.err.println("LinearSeparator: " + cplex.getStatus());

			cplex.end();
			return;
		}
		
		// Show the obtained inequality
//		System.out.println("l = " + _parent.getValor(_model.lVar(_cluster, _dimension)));
//		System.out.println("r = " + _parent.getValor(_model.rVar(_cluster, _dimension)));
//		
//		for(int i=0; i<_instance.getPoints(); ++i)
//			System.out.println("z[" + i + "] = " + _parent.getValor(_model.zVar(i, _cluster)));
//		
//		System.out.println();
//		System.out.println("a = " + cplex.getValue(a));
//		System.out.println("b = " + cplex.getValue(b));
//		System.out.println("beta = " + cplex.getValue(beta));
//		
//		for(int i=0; i<_instance.getPoints(); ++i)
//			System.out.println("alpha[" + i + "] = " + cplex.getValue(alpha[i]));
//		
//		System.out.println();
		
		double lhsval = cplex.getValue(a) * _parent.get(_model.rVar(_cluster, _dimension)) - cplex.getValue(b) * _parent.get(_model.lVar(_cluster, _dimension));
		double rhsval = -cplex.getValue(beta);

		for(int i=0; i<_instance.getPoints(); ++i)
			rhsval += cplex.getValue(alpha[i]) * _parent.get(_model.zVar(i, _cluster));

		if( _silent == false )
		{
			System.out.printf("%.2f * r", cplex.getValue(a));
			System.out.printf(" - %.2f * l >=", cplex.getValue(b));
			
			for(int i=0; i<_instance.getPoints(); ++i) if( Math.abs(cplex.getValue(alpha[i])) > 0.001 )
				System.out.printf(" + %.2f * z[%d]", cplex.getValue(alpha[i]), i);
	
			System.out.printf(" - %.2f", cplex.getValue(beta));
			System.out.printf(" (viol: %.2f)", rhsval-lhsval);
			System.out.println();
		}
		
		// Check validity
		if( _check == true )
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
					System.out.print("************** No es válida! a = ");
					
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
		
		if( rhsval-lhsval > 0.5 )
		{
			IloCplex master = _model.getCplex();
			IloNumExpr inequality = master.linearNumExpr();
			
			inequality = master.sum(inequality, master.prod(cplex.getValue(a), _model.rVar(_cluster, _dimension)));
			inequality = master.sum(inequality, master.prod(-cplex.getValue(b), _model.lVar(_cluster, _dimension)));
			
			for(int i=0; i<_instance.getPoints(); ++i)
				inequality = master.sum(inequality, master.prod(-cplex.getValue(alpha[i]), _model.zVar(i, _cluster)));
					
			_parent.addCut( master.ge(inequality, -cplex.getValue(beta)), IloCplex.CutManagement.UseCutForce );
		}
		
		cplex.end();
	}
	
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
}

