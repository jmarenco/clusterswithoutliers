package clustering;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class LinearSeparator extends IloCplex.UserCutCallback
{
	private RectangularModel _model;
	private Instance _instance;
	
	public LinearSeparator(RectangularModel model)
	{
		_model = model;
		_instance = model.getInstance();
	}
	
	@Override
	protected void main() throws IloException
	{
		if( !this.isAfterCutLoop() )
	        return;
		
		for(int i=0; i<_instance.getClusters(); ++i)
		for(int j=0; j<_instance.getDimension(); ++j)
			separate(i, j);
	}
	
	private void separate(int cluster, int dimension) throws IloException
	{
		// Create model and variables
		IloCplex cplex = new IloCplex();
		IloNumVar[] alpha = new IloNumVar[_instance.getPoints()];
		IloNumVar beta = cplex.numVar(-1e10, 1e10, "beta");
		IloNumVar a = cplex.numVar(0, 10, "a");
		IloNumVar b = cplex.numVar(0, 10, "b");
		
		for(int i=0; i<_instance.getPoints(); ++i)
			alpha[i] = cplex.numVar(0, 1e10, "alfa" + i);
		
		// Create objective function
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(-1.0, beta));
		fobj = cplex.sum(fobj, cplex.prod(-rVar(cluster, dimension), a));
		fobj = cplex.sum(fobj, cplex.prod(lVar(cluster, dimension), b));

		for(int i=0; i<_instance.getPoints(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(zVar(i,cluster), alpha[i]));
		
		cplex.addMaximize(fobj);
		
		// Create constraints for pairs of points
		for(int i=0; i<_instance.getPoints(); ++i)
		for(int j=i; j<_instance.getPoints(); ++j)
		{
			IloNumExpr lhs = cplex.linearNumExpr();
			lhs = cplex.sum(lhs, cplex.prod(1.0, beta));
			lhs = cplex.sum(lhs, cplex.prod(-max(i,j,dimension), a));
			lhs = cplex.sum(lhs, cplex.prod(min(i,j,dimension), b));
			
			for(int k=i; k<=j; ++k)
				lhs = cplex.sum(lhs, alpha[k]);
			
			cplex.addLe(lhs, 0, "c" + i);
		}
		
		// Create constraints for empty intervals
		IloNumExpr lhs1 = cplex.linearNumExpr();
		IloNumExpr lhs2 = cplex.linearNumExpr();

		lhs1 = cplex.sum(lhs1, cplex.prod(-1, beta));
		lhs1 = cplex.sum(lhs1, cplex.prod(-_instance.max(dimension), a));
		lhs1 = cplex.sum(lhs1, cplex.prod(_instance.max(dimension), b));
		
		lhs2 = cplex.sum(lhs2, cplex.prod(-1,  beta));
		lhs2 = cplex.sum(lhs2, cplex.prod(-_instance.min(dimension), a));
		lhs2 = cplex.sum(lhs2, cplex.prod(_instance.min(dimension), b));
		
		cplex.addLe(lhs1, 0, "inflim");
		cplex.addLe(lhs2, 0, "suplim");
		
		// Create normalization constraint
		IloNumExpr lhs3 = cplex.linearNumExpr();
		lhs3 = cplex.sum(lhs3, cplex.prod(-1.0, beta));

		for(int i=0; i<_instance.getPoints(); ++i)
			lhs3 = cplex.sum(lhs3, alpha[i]);
		
		cplex.addEq(lhs3, _instance.getPoints() + 1, "norm");
		
		// Solve
		cplex.exportModel("separator.lp");
		cplex.setOut(null);
		cplex.solve();
		
		// Show the obtained inequality
//		System.out.println("l = " + this.getValue(_model.lVar(cluster, dimension)));
//		System.out.println("r = " + this.getValue(_model.rVar(cluster, dimension)));
//		
//		for(int i=0; i<_instance.getPoints(); ++i)
//			System.out.println("z[i] = " + this.getValue(_model.zVar(i, cluster)));
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
		
		System.out.printf("%.2f * r", cplex.getValue(a));
		System.out.printf(" - %.2f * l >=", cplex.getValue(b));
		
		for(int i=0; i<_instance.getPoints(); ++i) if( Math.abs(cplex.getValue(alpha[i])) > 0.001 )
			System.out.printf(" + %.2f * z[%d]", cplex.getValue(alpha[i]), i);

		System.out.printf(" - %.2f", cplex.getValue(beta));
		
		double lhsval = cplex.getValue(a) * this.getValue(_model.rVar(cluster, dimension)) - cplex.getValue(b) * this.getValue(_model.lVar(cluster, dimension));
		double rhsval = -cplex.getValue(beta);

		for(int i=0; i<_instance.getPoints(); ++i)
			rhsval += cplex.getValue(alpha[i]) * this.getValue(_model.zVar(i, cluster));
		
		System.out.printf(" (viol: %.2f)", rhsval-lhsval);
		System.out.println();
		
		if( rhsval-lhsval > 0.01 )
		{
			IloCplex master = _model.getCplex();
			IloNumExpr inequality = master.linearNumExpr();
			
			inequality = master.sum(inequality, master.prod(cplex.getValue(a), _model.rVar(cluster, dimension)));
			inequality = master.sum(inequality, master.prod(-cplex.getValue(b), _model.lVar(cluster, dimension)));
			
			for(int i=0; i<_instance.getPoints(); ++i)
				inequality = master.sum(inequality, master.prod(-cplex.getValue(alpha[i]), _model.zVar(i, cluster)));
					
			this.add( master.ge(inequality, -cplex.getValue(beta)), IloCplex.CutManagement.UseCutForce );
		}
		
		cplex.end();
	}
	
	public double min(int start, int end, int dimension)
	{
		double ret = _instance.getPoint(start).get(dimension);
		
		for(int i=start+1; i<=end; ++i)
			ret = Math.min(ret, _instance.getPoint(i).get(dimension));
		
		return ret;
	}
	
	public double max(int start, int end, int dimension)
	{
		double ret = _instance.getPoint(start).get(dimension);
		
		for(int i=start+1; i<=end; ++i)
			ret = Math.max(ret, _instance.getPoint(i).get(dimension));
		
		return ret;
	}

	public double zVar(int point, int cluster) throws IloException
	{
		return this.getValue(_model.zVar(point, cluster));
	}
	
	public double rVar(int cluster, int dimension) throws IloException
	{
		return this.getValue(_model.rVar(cluster, dimension));
	}
	
	public double lVar(int cluster, int dimension) throws IloException
	{
		return this.getValue(_model.lVar(cluster, dimension));
	}
}

