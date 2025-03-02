package flowModel;

import java.util.ArrayList;
import java.util.Arrays;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Solution;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.Status;
import ilog.cplex.IloCplex.UnknownObjectException;
import incremental.BlackBoxClusteringSolver;

public class FlowModel implements BlackBoxClusteringSolver
{
	// Instance
	private Instance _instance;
	
	// Solution
	private ArrayList<Cluster> _clusters;
	
	// Parameters
	private int _maxTime = 3600;
	private static boolean _verbose = true;
	private static boolean _summary = false;
	
	// Model sizes
	private int p; // Points
	private int n; // Clusters
	private int o; // Outliers
	private int d; // Dimension

	// Solver
	private IloCplex cplex;

    // Variables
	private IloNumVar[][] z;
	private IloNumVar[][][] wr;
	private IloNumVar[][][] wl;
	
	// Indices de puntos ordenados en cada dimension
	private int[][] _sorted;

	// Clock
	private Clock _clock;
	
	// Last LB
	private double _lastLB;

	public String getSolverName()
	{
		return "FM";
	}

	public FlowModel(Instance instance)
	{
		init(instance);
	}

	private void init(Instance instance) 
	{
		_instance = instance;

		p = _instance.getPoints();
		n = _instance.getClusters();
		o = _instance.getOutliers();
		d = _instance.getDimension();
		
		_sorted = new int[d][p];

		for(int t=0; t<d; ++t)
		{
			Integer[] indices = new Integer[p];
			for(int i=0; i<p; ++i) 
				indices[i] = i;

			final int final_t = t;
			Arrays.sort(indices, (i, j) -> Double.compare( _instance.getPoint(i).get(final_t), _instance.getPoint(j).get(final_t)));
			
			for(int i=0; i<p; ++i)
				_sorted[t][indices[i]] = i;
		}
	}
	
	public void setMaxTime(int value)
	{
		_maxTime = value;
	}

	public Solution solve() throws Exception
	{
		return solve(_instance);
	}
	
	public Solution solve(Instance ins) throws Exception
	{
		Solution trivial_solution = Solution.withAllPoints(ins);
		return solve(ins, trivial_solution);
	}

	public Solution solve(Instance ins, Solution initial_solution) throws Exception
	{
		init(ins);
		
		_clock = new Clock(_maxTime);
		_clock.start();
		
		createSolver();
		createVariables();
	    createClusteringConstraints();
		createOutliersConstraint();
	    createBindingConstraints();
	    createOrderingConstraints();
		createObjective();
		addInitialSolution(initial_solution);

		solveModel();
    	obtainSolution();
	    closeSolver();
	    
	    _clock.stop();
	    
	    return new Solution(_clusters);
	}

	private void createSolver() throws IloException
	{
		if (cplex != null)
			cplex.end();
		
		cplex = new IloCplex();
		
		if( _verbose == false )
		{
			cplex.setOut(null);
			cplex.setWarning(null);
		}
	}

	private void createVariables() throws IloException
	{
		z = new IloNumVar[p][n];
		wr = new IloNumVar[d][n][p];
		wl = new IloNumVar[d][n][p];
		
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = cplex.boolVar("z" + i + "_" + j);

		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
	    	wr[t][j][i] = cplex.boolVar("wr" + t + "_" + j + "_" + i);
	    	wl[t][j][i] = cplex.boolVar("wl" + t + "_" + j + "_" + i);
		}
	}

	private void createClusteringConstraints() throws IloException
	{
		for(int i=0; i<p; ++i)
	    {
			IloNumExpr lhs = cplex.linearIntExpr();
			
		    for(int j=0; j<n; ++j)
		    	lhs = cplex.sum(lhs, z[i][j]);

		    if( o == 0 )
		    	cplex.addEq(lhs, 1, "clus" + i);
		    else
		    	cplex.addLe(lhs, 1, "clus" + i);
	    }
	}

	private void createOutliersConstraint() throws IloException
	{
		if( o == 0 )
			return;
		
		IloNumExpr lhsOut = cplex.linearIntExpr();
		
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		 	lhsOut = cplex.sum(lhsOut, z[i][j]);
		    
	    cplex.addGe(lhsOut, p-o, "out");
	}
	
	private void createBindingConstraints() throws IloException
	{
		for(int i=0; i<p-1; ++i)
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs1 = cplex.linearIntExpr();
			IloNumExpr lhs2 = cplex.linearIntExpr();
			
			lhs1 = cplex.sum(lhs1, wl[t][j][i]);
			lhs2 = cplex.sum(lhs2, wr[t][j][i]);
			lhs1 = cplex.sum(lhs1, cplex.prod(-1, wl[t][j][i+1]));
			lhs2 = cplex.sum(lhs2, cplex.prod(-1, wr[t][j][i+1]));
			
			cplex.addLe(lhs1, 0, "cl" + t + "_" + j + "_" + i);
			cplex.addGe(lhs2, 0, "cr" + t + "_" + j + "_" + i);
		}

		for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs1 = cplex.linearIntExpr();
			IloNumExpr lhs2 = cplex.linearIntExpr();
			
			lhs1 = cplex.sum(lhs1, z[i][j]);
			lhs2 = cplex.sum(lhs2, z[i][j]);
			lhs1 = cplex.sum(lhs1, cplex.prod(-1, wl[t][j][sigma(i,t)]));
			lhs2 = cplex.sum(lhs2, cplex.prod(-1, wr[t][j][sigma(i,t)]));
			
			cplex.addLe(lhs1, 0, "zl" + t + "_" + j + "_" + i);
			cplex.addLe(lhs2, 0, "zr" + t + "_" + j + "_" + i);
		}
	}

	private void createOrderingConstraints() throws IloException
	{
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			IloNumExpr lhs = cplex.linearIntExpr();
			
			for(int i=0; i<p; ++i)
			{
				lhs = cplex.sum(lhs, wl[t][j][i]);
				lhs = cplex.sum(lhs, wr[t][j][i]);
			}

			cplex.addGe(lhs, p+1, "ord" + t + "_" + j);
		}
	}

	private void createObjective() throws IloException
	{
		IloNumExpr fobj = cplex.linearNumExpr();

		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			fobj = cplex.sum(fobj, cplex.prod(c(0,t), wr[t][j][0]));
			
			for(int k=1; k<p; ++k)
				fobj = cplex.sum(fobj, cplex.prod(c(k,t) - c(k-1,t), wr[t][j][k]));
				
			fobj = cplex.sum(fobj, cplex.prod(-c(p-1,t), wl[t][j][p-1]));

			for(int k=0; k<p-1; ++k)
				fobj = cplex.sum(fobj, cplex.prod(c(k+1,t) - c(k,t), wl[t][j][k]));
		}
		
		cplex.addMinimize(fobj);
	}
	private void addInitialSolution(Solution solution) throws IloException
	{
		// Not implemented yet.
	}

	

	private void solveModel() throws IloException
	{
		cplex.setParam(IntParam.TimeLimit, _clock.remaining());
		cplex.solve();
		
		_lastLB = cplex.getBestObjValue();

		if( _summary == false )
		{
			System.out.println("Status: " + cplex.getStatus());
			System.out.println("Objective: " + String.format("%6.4f", cplex.getObjValue()));
			System.out.println("Time: " + String.format("%6.2f", _clock.elapsed()));
			System.out.println("Nodes: " + cplex.getNnodes());
			System.out.println("Gap: " + ((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) : "  ****"));
			System.out.println("Cuts: " + cplex.getNcuts(IloCplex.CutType.User));
		}
		else
		{
			System.out.print(_instance.getName() + " | Flow | ");
			System.out.print(cplex.getStatus() + " | ");
			System.out.print("Obj: " + String.format("%6.4f", cplex.getObjValue()) + " | ");
			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
			System.out.print(cplex.getNnodes() + " nodes | ");
			System.out.print(((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) + " % | " : "  **** | "));
			System.out.print(cplex.getNcuts(IloCplex.CutType.User) + " cuts | ");
			System.out.print("MT: " + _maxTime + " | ");
			System.out.println();
		}
	}

	private void obtainSolution() throws IloException, UnknownObjectException
	{
		_clusters = new ArrayList<Cluster>();
    	if( cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible )
		{
	    	for(int j=0; j<n; ++j)
	    		_clusters.add(new Cluster());
	    		
			for(int i=0; i<p; ++i)
		    for(int j=0; j<n; ++j) if( cplex.getValue(z[i][j]) > 0.9 )
		    {
//		    	System.out.println("z" + i + "_" + j + " = " + cplex.getValue(z[i][j]));
		    	_clusters.get(j).add(_instance.getPoint(i));
		    }

//			for(int j=0; j<n; ++j)
//			for(int t=0; t<d; ++t)
//			for(int i=0; i<p; ++i)
//			{
//				System.out.println("wl" + t + "_" + j + "_" + i + " = " + cplex.getValue(wl[t][j][i]));
//				System.out.println("wr" + t + "_" + j + "_" + i + " = " + cplex.getValue(wr[t][j][i]));
//			}
		}
    }
	
	public void closeSolver()
	{
		cplex.end();
	}
	
	private int sigma(int i, int t)
	{
		return _sorted[t][i];
	}
	
	private double c(int i, int t)
	{
		for(int l=0; l<p; ++l) if( _sorted[t][l] == i )
			return _instance.getPoint(l).get(t);
		
		throw new RuntimeException("FlowModel.c()");
	}

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public IloNumVar zVar(int point, int cluster)
	{
		return z[point][cluster];
	}
	
	public IloNumVar wrVar(int dimension, int cluster, int point)
	{
		return wr[dimension][cluster][point];
	}
	
	public IloNumVar wlVar(int dimension, int cluster, int point)
	{
		return wl[dimension][cluster][point];
	}
	
	public IloCplex getCplex()
	{
		return cplex;
	}
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}

	public int getNSolutions() throws Exception 
	{
		return cplex.getSolnPoolNsolns();
	}

	public double getObjValueOfSolutionN(int s) throws Exception
	{
		return cplex.getObjValue(s);
	}

	public Solution getSolutionNumber(int s) throws Exception 
	{
		ArrayList<Cluster> _solclusters = new ArrayList<Cluster>();
		
    	for(int j=0; j<n; ++j)
    	{
    		Cluster cluster = new Cluster();
	    		
			for(int i=0; i<p; ++i) if( cplex.getValue(z[i][j], s) > 0.9 )
		    	cluster.add(_instance.getPoint(i));
    		
    		_solclusters.add(cluster);
    	}

		return new Solution(_solclusters);
	}

	@Override
	public double getLastLB()
	{
		return _lastLB;
	}
}
