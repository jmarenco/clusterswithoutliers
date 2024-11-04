package standardModelCpsat;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.DoubleLinearExpr;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;

import general.Clock;
import general.Cluster;
import general.Instance;
import general.Logger;
import general.RectangularCluster;
import general.Results;
import general.Solution;
import incremental.BlackBoxClusteringSolver;


public class RectangularModelCpsatBinary implements BlackBoxClusteringSolver {
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
	private Literal[][][] r;
	private Literal[][][] l;

	// Coordinates
	private double[][] sorted_coords_per_dim;
	private int[][] rank_point_per_dim;
	
	private Clock _clock;

	private boolean _log_solution = true;

	private double _last_lb = 0.0;
	
	public String getSolverName()
	{
		return "CPSATBIN";
	}

	public RectangularModelCpsatBinary(Instance instance)
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
		sortCoords();
	}
	
	private void sortCoords()
	{
		sorted_coords_per_dim = new double[d][p];
		rank_point_per_dim = new int[d][p];

		for(int t=0; t<d; ++t) {
			Integer[] indices = new Integer[p];
			for(int i=0; i<p; ++i)
			{
				indices[i] = i;
			}
			final int final_t = t;
			Arrays.sort(indices, (i, j) -> Double.compare( _instance.getPoint(i).get(final_t), _instance.getPoint(j).get(final_t)));
			for(int i=0; i<p; ++i) 
			{						
				rank_point_per_dim[t][indices[i]] = i;
				sorted_coords_per_dim[t][i] = _instance.getPoint(indices[i]).get(t);
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
		return solve(_instance);
	}

	public Solution solve(Instance ins) throws Exception
	{
		init(ins);
		
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
	    solver.getParameters().setLogSearchProgress(_verbose);
	}

	private void createVariables() throws Exception
	{
		z = new Literal[p][n];
		r = new Literal[n][d][p];
		l = new Literal[n][d][p];

		for(int i=0; i<p; ++i)
	    for(int j=0; j<n; ++j)
	    	z[i][j] = model.newBoolVar("z" + i + "_" + j);

		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		for(int i=0; i<p; ++i) {
				r[j][t][i] = model.newBoolVar("r" + j + "_" + t + "_rank_" + i);
				l[j][t][i] = model.newBoolVar("l" + j + "_" + t + "_rank_" + i);
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
            model.addImplication(z[i][j], r[j][t][rank_point_per_dim[t][i]]);
            model.addImplication(z[i][j], l[j][t][rank_point_per_dim[t][i]]);
		}
	}

	private void createOrderingConstraints() throws Exception
	{
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		for(int i=0; i<p-1; ++i)
		{
			model.addImplication(l[j][t][i], l[j][t][i+1]);
			model.addImplication(r[j][t][i+1], r[j][t][i]);
		}
	    
	    for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t)
		{
			LinearExprBuilder all_borders = LinearExpr.newBuilder();
			for(int i=0; i<p; ++i) 
			{
				all_borders.add(l[j][t][i]);
				all_borders.add(r[j][t][i]);
			}
			model.addGreaterOrEqual(all_borders, p+1);
		}
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
		Literal[] flatten_vars = new Literal[2*n*d*p];
		double[] flatten_coef = new double[2*n*d*p];
		for(int j=0; j<n; ++j)
		for(int t=0; t<d; ++t) {
			int offset = j*d*p+t*p;
			flatten_vars[offset + 0] = r[j][t][0];
			flatten_coef[offset + 0] = sorted_coords_per_dim[t][0];
		    for(int i=1; i<p; ++i)
		    {
				flatten_vars[offset + i] = r[j][t][i];
				flatten_coef[offset + i] = (sorted_coords_per_dim[t][i]-sorted_coords_per_dim[t][i-1]);		    	
		    }
		    offset += n*d*p;
		    for(int i=0; i<p-1; ++i)
		    {
				flatten_vars[offset + i] = l[j][t][i];
				flatten_coef[offset + i] = (sorted_coords_per_dim[t][i+1]-sorted_coords_per_dim[t][i]);		    	
		    }
			flatten_vars[offset + p-1] = l[j][t][p-1];
			flatten_coef[offset + p-1] = -sorted_coords_per_dim[t][p-1];
		}
		DoubleLinearExpr objective = new DoubleLinearExpr(flatten_vars, flatten_coef, 0.0);
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
		

		_last_lb = (double)solver.bestObjectiveBound();
		
		if( _summary == false )
		{
			System.out.println("Status: " + status.getNumber());
			System.out.println("Objective: " + String.format("%6.4f", (double)solver.objectiveValue()));
			System.out.println("Time: " + String.format("%6.2f", _clock.elapsed()));
			System.out.println("Nodes: " + solver.numBranches());
			System.out.println("Gap: " + ((gap >= 0.0)? String.format("%6.2f", 100 * gap) : "  ****"));
			System.out.println("Cuts: " + "  *****");
		}
		else
		{
			System.out.println("Status: " + status.getNumber());
			System.out.println("Objective: " + String.format("%6.4f", (double)solver.objectiveValue()));
			System.out.println("Time: " + String.format("%6.2f", _clock.elapsed()));
			System.out.println("Nodes: " + solver.numBranches());
			System.out.println("Gap: " + ((gap >= 0.0)? String.format("%6.2f", 100 * gap) : "  ****"));
			System.out.println("Cuts: " + "  *****");
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
    		ub = (double)solver.objectiveValue();
	    	for(int j=0; j<n; ++j)
	    	{
	    		RectangularCluster cluster = new RectangularCluster(d);
				for(int t=0; t<d; ++t)
				{
					for (int i=0; i<p ; i++)
					{
						if (solver.value(l[j][t][i]) > 0)
						{
							cluster.setMin(t, sorted_coords_per_dim[t][i]);
							break;
						}
						
					}
					for (int i=p-1; i >= 0 ; i--)
					{
						if (solver.value(r[j][t][i]) > 0)
						{
							cluster.setMax(t, sorted_coords_per_dim[t][i]);
							break;
						}
						
					}

					System.out.println(" Optimal dim " + t + " [" + cluster.getMin(t) + "," + cluster.getMax(t) + "]");
				}
	    		_clusters.add(cluster);
	    	}
	    		
			for(int i=0; i<p; ++i)
		    for(int j=0; j<n; ++j) if( solver.value(z[i][j]) > 0.9 )
		    {
		    	_clusters.get(j).add(_instance.getPoint(i));
		    }

		}

    	
		// Log the solution
    	if (_log_solution)
    	{
			Results.Status stat = (status == CpSolverStatus.OPTIMAL)? Results.Status.OPTIMAL : (status == CpSolverStatus.FEASIBLE? Results.Status.FEASIBLE : Results.Status.NOSOLUTION);
			double lb =(double) solver.bestObjectiveBound();
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
	
	public Literal rVar(int cluster, int dimension, int pos)
	{
		return r[cluster][dimension][pos];
	}
	
	public Literal lVar(int cluster, int dimension, int pos)
	{
		return l[cluster][dimension][pos];
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

	// To implement. For now, returns the optimal solution.
	public int getNSolutions() throws Exception 
	{
		return 1;
	}

	public double getObjValueOfSolutionN(int s) throws Exception
	{
		return solver.objectiveValue();
	}

	public Solution getSolutionNumber(int s) throws Exception 
	{
		return new Solution(_clusters);	
	}
	public void closeSolver() 
	{ }


	public void keepAlive() 
	{ }


	public void setStrongBinding(boolean b) 
	{ }
}