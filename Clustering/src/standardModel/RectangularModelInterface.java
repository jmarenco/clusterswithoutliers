package standardModel;

import general.Instance;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public interface RectangularModelInterface 
{

	Instance getInstance();

	IloCplex getCplex();

	IloNumVar rVar(int cluster, int _dimension1);

	IloNumVar lVar(int cluster, int _dimension1);

	IloNumVar zVar(int i, int cluster);
}
