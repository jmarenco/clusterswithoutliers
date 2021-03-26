package clustering;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.Status;

public class CplexTest
{
	public static void main(String[] args) throws IloException
	{
		// Create solver
		IloCplex cplex = new IloCplex();

	    // Create variables
		IloNumVar x0 = cplex.numVar(0, 1000, "x0");
		IloNumVar x1 = cplex.numVar(0, 1000, "x1");
		IloNumVar x2 = cplex.numVar(0, 1000, "x2");
		
	    // Create constraints
		IloNumExpr lhs1 = cplex.linearIntExpr();
		lhs1 = cplex.sum(lhs1, x0);
		lhs1 = cplex.sum(lhs1, x1);
		cplex.addLe(lhs1, 1, "c1");
	    
		IloNumExpr lhs2 = cplex.linearIntExpr();
		lhs2 = cplex.sum(lhs2, x0);
		lhs2 = cplex.sum(lhs2, x2);
		cplex.addLe(lhs2, 1, "c1");
		
		IloNumExpr lhs3 = cplex.linearIntExpr();
		lhs3 = cplex.sum(lhs3, cplex.prod(7, x0));
		lhs3 = cplex.sum(lhs3, cplex.prod(3, x1));
		lhs3 = cplex.sum(lhs3, cplex.prod(4, x2));
		cplex.addGe(lhs3, 6, "c3");
		
		// Objective
		IloNumExpr fobj = cplex.linearNumExpr();
		fobj = cplex.sum(fobj, cplex.prod(1.38, x1));
		fobj = cplex.sum(fobj, cplex.prod(14.208, x0));
		fobj = cplex.sum(fobj, cplex.prod(4.54, x2));
		cplex.addMinimize(fobj);
		
		// Solve model
		cplex.solve();

		System.out.println();
		System.out.println(cplex.getStatus());
		
    	System.out.println(x0 + " = " + cplex.getValue(x0) + ", RC = " + cplex.getReducedCost(x0));
    	System.out.println(x1 + " = " + cplex.getValue(x1) + ", RC = " + cplex.getReducedCost(x1));
    	System.out.println(x2 + " = " + cplex.getValue(x2) + ", RC = " + cplex.getReducedCost(x2));
	    
	    cplex.end();
	}
}
