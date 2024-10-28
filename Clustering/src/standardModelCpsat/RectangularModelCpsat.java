package standardModelCpsat;

import java.util.ArrayList;
import java.util.Arrays;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Logger;
import general.RectangularCluster;
import general.Results;
import general.Solution;

import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExprBuilder;
//import com.google.ortools.sat.DoubleLinearExpr;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;




public class RectangularModelCpsat {
	// Instance
	private Instance _instance;
	
	// Solution
	private ArrayList<Cluster> _clusters;
	
	// Parameters
	private int _maxTime = 3600;
	
	private static boolean _verbose = true;
	private static boolean _summary = false;
	
	public static enum SymmetryBreaking { None, Size, IndexSum, OrderedStart }; 
	public static enum Objective { Span, Area }; 
	
	// Model sizes
	private int p; // Points
	private int n; // Clusters
	private int o; // Outliers
	private int d; // Dimension

	// Solver
	private CpModel model;
	private CpSolver solver = new CpSolver();
	private CpSolverStatus status;

    // Variables
	private Literal[][] z;
	private IntVar[][] r;
	private IntVar[][] l;

	// Coordinates
	private long[][] sorted_coords_per_dim;
//	private int[][] rank_point_per_dim;
	private double round_factor = 1e10;
	
	private Clock _clock;

	private boolean _log_solution = true;

	private double _last_lb = 0.0;
	
	private long toLong(double value)
	{
		return (long)(value*round_factor);
	}


	public RectangularModelCpsat(Instance instance)
	{
		_instance = instance;

		p = _instance.getPoints();
		n = _instance.getClusters();
		o = _instance.getOutliers();
		d = _instance.getDimension();
		sortCoords();
	}
	
	private void sortCoords()
	{
		sorted_coords_per_dim = new long[d][p];
//		rank_point_per_dim = new int[d][p];

		for(int t=0; t<d; ++t) {
			Integer[] indices = new Integer[p];
			for(int i=0; i<p; ++i) {
				indices[i] = i;
			}
			final int final_t = t;
			Arrays.sort(indices, (i, j) -> Double.compare( _instance.getPoint(i).get(final_t), _instance.getPoint(j).get(final_t)));
			for(int i=0; i<p; ++i) {
//				System.out.println("Point " + i + " dim " + t + " val=" +  toLong(_instance.getPoint(indices[i]).get(t)) + " rank " + indices[i]);
						
//				rank_point_per_dim[t][indices[i]] = i;
				sorted_coords_per_dim[t][i] = toLong(_instance.getPoint(indices[i]).get(t));
            }
		}
	}
	
	public void setMaxTime(int value)
	{
		_maxTime = value;
	}
	
	public void setLogSolution(boolean _log_solution) 
	{
		this._log_solution = _log_solution;
	}

	public Solution solve() throws Exception
	{
		_clock = new Clock(_maxTime);
		_clock.start();
		
		createSolver();
		createVariables();
	    createClusteringConstraints();
	    createBindingConstraints();
	    createOrderingConstraints();
		createOutliersConstraint();
		createObjective();
					
		
		solveModel();
    	obtainSolution();
	    
	    _clock.stop();
	    
	    return new Solution(_clusters);
	}

	private void createSolver() throws Exception
	{
		model = new CpModel();
	    solver.getParameters().setLogToStdout(_verbose);
	}

	private void createVariables() throws Exception
	{
		z = new Literal[p][n];
		r = new IntVar[n][d];
		l = new IntVar[n][d];

		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = model.newBoolVar("z" + i + "_" + j);

		for(int t=0; t<d; ++t) {
			Domain domain = Domain.fromValues(sorted_coords_per_dim[t]);
			for(int j=0; j<n; ++j) {
				r[j][t] = model.newIntVarFromDomain(domain, "r" + j + "_" + t);
				l[j][t] = model.newIntVarFromDomain(domain, "l" + j + "_" + t);
			}
		}
	}

	private void createClusteringConstraints() throws Exception
	{
		for(int i=0; i<p; ++i)
			model.addAtMostOne(z[i]);
	}
	
	private void createBindingConstraints() throws Exception
	{
		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
            model.addLessOrEqual(l[j][t], toLong(_instance.getPoint(i).get(t))).onlyEnforceIf(z[i][j]);
            model.addGreaterOrEqual(r[j][t], toLong(_instance.getPoint(i).get(t))).onlyEnforceIf(z[i][j]);
		}
	}

	private void createOrderingConstraints() throws Exception
	{
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
			model.addGreaterOrEqual(r[j][t], l[j][t]);
	}

	private void createOutliersConstraint() throws Exception
	{
		LinearExprBuilder all_points = LinearExpr.newBuilder();
	    for(int i=0; i<p; ++i)
		for(int j=0; j<n; ++j)
			all_points.add(z[i][j]);
	    model.addGreaterOrEqual(all_points, p-o);
	}


	private void createObjective() throws Exception
	{
			createLinearObjective();
	}
		
	private void createLinearObjective() throws Exception
	{
		LinearExprBuilder objective = LinearExpr.newBuilder();
		
//		IntVar[] flatten_vars = new IntVar[2*n*d];
//		double[] flatten_coef = new double[2*n*d];
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t) {
			objective.addTerm(r[j][t], 1);
			objective.addTerm(l[j][t], -1);
//			flatten_vars[2*(j*d+t)] = r[j][t];
//			flatten_coef[2*(j*d+t)] = 1.0;
//			flatten_vars[2*(j*d+t)+1] = l[j][t];
//			flatten_coef[2*(j*d+t)+1] = -1.0;
		}
		
//		DoubleLinearExpr objective = new DoubleLinearExpr(flatten_vars, flatten_coef);
		model.minimize(objective);
	}

	private void solveModel() throws Exception
	{

	    solver.getParameters().setMaxTimeInSeconds(_clock.remaining());

		status = solver.solve(model);
		double gap = -1.0;
		if ((status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE)) {
			gap = (solver.objectiveValue() - solver.bestObjectiveBound())/(solver.bestObjectiveBound()+1e-6);
		}
		

		_last_lb = solver.bestObjectiveBound();
		
		if( _summary == false )
		{
			System.out.println("Status: " + status.getNumber());
			System.out.println("Objective: " + String.format("%6.4f", (double)solver.objectiveValue()/round_factor));
			System.out.println("Time: " + String.format("%6.2f", _clock.elapsed()));
			System.out.println("Nodes: " + solver.numBranches());
			System.out.println("Gap: " + ((gap >= 0.0)? String.format("%6.2f", 100 * gap) : "  ****"));
			System.out.println("Cuts: " + "  *****");
		}
		else
		{
//			System.out.print(_instance.getName() + " | Std | ");
//			System.out.print(cplex.getStatus() + " | ");
//			System.out.print("Obj: " + String.format("%6.4f", cplex.getObjValue()) + " | ");
//			System.out.print(String.format("%6.2f", _clock.elapsed()) + " sec. | ");
//			System.out.print(cplex.getNnodes() + " nodes | ");
//			System.out.print(((cplex.getStatus() == Status.Optimal || cplex.getStatus() == Status.Feasible) && cplex.getMIPRelativeGap() < 1e30 ? String.format("%6.2f", 100 * cplex.getMIPRelativeGap()) + " % | " : "  **** | "));
//			System.out.print(cplex.getNcuts(IloCplex.CutType.User) + " cuts | ");
//			System.out.print("MR: " + Separator.getMaxRounds() + " | ");
//			System.out.print("SF: " + Separator.getSkipFactor() + " | ");
//			System.out.print("Cut execs: " + separator.getExecutions() + " | ");
//			System.out.print(Separator.getCutAndBranch() ? "C&B | " : "    | ");
//			System.out.print("MT: " + _maxTime + " | ");
//			System.out.print("SB: " + (_symmetryBreaking == SymmetryBreaking.Size ? "Size" : (_symmetryBreaking == SymmetryBreaking.IndexSum ? "Idx " : (_symmetryBreaking == SymmetryBreaking.OrderedStart ? "OrSt" : "    "))) + " | "); 
//			System.out.print("Thr: " + (Separator.getStrategy() == 0 ? LinearSeparator.getThreshold() : (Separator.getStrategy() == 1 ? LinearSeparatorSparse.getThreshold() : LinearSeparatorRestricted.getThreshold())) + " | ");
//			System.out.print("Ss: " + Separator.getStrategy() + (Separator.getStrategy() == 4 ? " : " + SquareSeparator.getSparsingRatio() : "" ) + " | ");
//			System.out.println();
		}
	}

	private void obtainSolution() throws Exception
	{
		_clusters = new ArrayList<Cluster>();
		double ub = Double.MAX_VALUE;
		
    	if( status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE )
		{
    		ub = (double)solver.objectiveValue()/round_factor;
	    	for(int j=0; j<n; ++j)
	    	{
	    		RectangularCluster cluster = new RectangularCluster(d);
				for(int t=0; t<d; ++t)
				{
//					System.out.println(" Optimal dim " + t + " [" + solver.value(l[j][t]) + "," + solver.value(r[j][t]) + "]");
					cluster.setMin(t, (double)solver.value(l[j][t])/round_factor);
					cluster.setMax(t, (double)solver.value(r[j][t])/round_factor);
				}
	    		_clusters.add(cluster);
	    	}
	    		
			for(int i=0; i<p; ++i)
		    for(int j=0; j<n; ++j) if( solver.value(z[i][j]) > 0.9 )
		    {
//		    	System.out.println("z" + i + "_" + j + " = " + solver.value(z[i][j]));
		    	_clusters.get(j).add(_instance.getPoint(i));
		    }

//			for(int j=0; j<n; ++j)
//			for(int t=0; t<d; ++t)
//			{
//				System.out.println("l" + j + "_" + t + " = " + cplex.getValue(l[j][t]));
//				System.out.println("r" + j + "_" + t + " = " + cplex.getValue(r[j][t]));
//			}
		}
    	
		// Log the solution
    	if (_log_solution)
    	{
			Results.Status stat = (status == CpSolverStatus.OPTIMAL)? Results.Status.OPTIMAL : (status == CpSolverStatus.FEASIBLE? Results.Status.FEASIBLE : Results.Status.NOSOLUTION);
			double lb =(double) solver.bestObjectiveBound()/round_factor;
			Logger.log(_instance, "CMP", new Results(new Solution(_clusters), stat, lb, ub, _clock.elapsed(), 0, 0, _instance.getPoints()));
    	}
    }
	

	public ArrayList<Cluster> getClusters()
	{
		return _clusters;
	}
	
	public Instance getInstance()
	{
		return _instance;
	}
	
	public Literal zVar(int point, int cluster)
	{
		return z[point][cluster];
	}
	
	public IntVar rVar(int cluster, int dimension)
	{
		return r[cluster][dimension];
	}
	
	public IntVar lVar(int cluster, int dimension)
	{
		return l[cluster][dimension];
	}
	
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}
	

	public double getLastLB() 
	{
		return _last_lb ;
	}

	public int getNSolutions() throws Exception 
	{
//		return cplex.getSolnPoolNsolns();
		return 1;
	}

	public double getObjValueOfSolutionN(int s) throws Exception
	{
//		return cplex.getObjValue(s);
		return solver.objectiveValue();
	}

//	public Solution getSolutionNumber(int s) throws Exception 
//	{
//		_clusters = new ArrayList<Cluster>();
//    	for(int j=0; j<n; ++j)
//    	{
//    		RectangularCluster cluster = new RectangularCluster(d);
//    		
//			for(int t=0; t<d; ++t)
//			{
//				cluster.setMin(t, cplex.getValue(l[j][t], s));
//				cluster.setMax(t, cplex.getValue(r[j][t], s));
//			}
//    		
//    		_clusters.add(cluster);
//    	}
//    		
//		for(int i=0; i<p; ++i)
//	    for(int j=0; j<n; ++j) if( cplex.getValue(z[i][j], s) > 0.9 )
//	    {
//	    	_clusters.get(j).add(_instance.getPoint(i));
//	    }
//
//		return new Solution(_clusters);
//	}

}
