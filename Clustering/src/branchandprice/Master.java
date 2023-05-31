package branchandprice;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import general.Instance;
import general.Cluster;

public class Master
{
	private Instance _instance;
	private IloCplex cplex;
    private IloObjective _obj; // Objective function
    private IloRange[] _binding; // Constraints
    private IloRange _numberOfClusters;
    private IloRange _numberOfOutliers;
    private IloRange[] _allConstraints;
    private IloNumVar[] _y;
    private ArrayList<Cluster> _columns;
    private Map<IloNumVar, Cluster> _variables;
    private ArrayList<BranchingDecision> _branchings;

    public Master(Instance instance)
    {
    	_instance = instance;
    	_columns = new ArrayList<Cluster>();
    	_branchings = new ArrayList<BranchingDecision>();
    }

    // Builds the master model
    private void buildModel()
    {
    	try
        {
        	_variables = new HashMap<IloNumVar, Cluster>();

        	// Create Cplex
            cplex = new IloCplex();
            cplex.setOut(null);

            // Define objective
            _obj = cplex.addMinimize();

            // Create y variables
            _y = new IloNumVar[_instance.getPoints()];
            for(int i=0; i<_instance.getPoints(); i++)
            	_y[i] = cplex.numVar(0, 1, "y" + i);

            // Define binding constraints
            _binding = new IloRange[_instance.getPoints()];
            for(int i=0; i<_instance.getPoints(); i++)
            {
            	IloNumExpr lhs = cplex.linearIntExpr();
            	lhs = cplex.sum(lhs, cplex.prod(-1, _y[i]));
                _binding[i] = cplex.addGe(lhs, 0, "bind" + i);
            }
            
            // Define constraint on the number of clusters
            _numberOfClusters = cplex.addGe(cplex.linearIntExpr(), -_instance.getClusters(), "clus");
            
            // Define constraint on the number of outliers
            IloNumExpr lhs = cplex.linearIntExpr();
            for(int i=0; i<_instance.getPoints(); i++)
            	lhs = cplex.sum(lhs, _y[i]);
            
            _numberOfOutliers = cplex.addGe(lhs, _instance.getPoints() - _instance.getOutliers(), "out");
            
            // Collects all constraints into a single array
            _allConstraints = new IloRange[_instance.getPoints() + 2];

            for(int i=0; i<_instance.getPoints(); i++)
            	_allConstraints[i] = _binding[i];
            
            _allConstraints[_instance.getPoints()] = _numberOfClusters;
            _allConstraints[_instance.getPoints() + 1] = _numberOfOutliers;
            
            // Adds all variables from existing columns
            for(Cluster cluster: _columns)
            {
            	if( _branchings.stream().allMatch(b -> b.isCompatible(cluster)) )
            		addColumnToModel(cluster);
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }

    // Solve the master problem
    public boolean solve(long timeLimit)
    {
        try
        {
            // Set time limit
            double timeRemaining = Math.max(1, (timeLimit-System.currentTimeMillis()) / 1000.0);
            cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining); // set time limit in seconds

            // Solve the model
            if( !cplex.solve() || cplex.getStatus() != IloCplex.Status.Optimal )
            {
                if( cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim ) //Aborted due to time limit
                    return false;
                else
                    throw new RuntimeException("Master problem solve failed! Status: " + cplex.getStatus());
            }
            else
            {
                System.out.println("Master solved - obj: " + cplex.getObjValue());
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return true;
    }

    // Extracts information from the master problem required by the pricing problems
    public double[] getDuals()
    {
    	double[] ret = null;
    	
        try
        {
            ret = cplex.getDuals(_allConstraints);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    // Adds columns ensuring that a feasible solution exists
    public void addFeasibleColumns()
    {
    	addColumn(Cluster.withAllPoints(_instance));
    }

    // Adds a new column to the master problem
    public void addColumn(Cluster cluster)
    {
        try
        {
        	addColumnToModel(cluster);
            _columns.add(cluster);
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
    }
    
    private void addColumnToModel(Cluster cluster) throws IloException
    {
		if( _columns.contains(cluster) )
			throw new RuntimeException("Duplicated column added to master! " + cluster);

		// Register column with objective
    	System.out.println("Registering column - cost: " + cluster.totalSpan() + " - " + cluster);
        IloColumn iloColumn = cplex.column(_obj, cluster.totalSpan());

        // Register column with the constraints
        for(int i=0; i<_instance.getPoints(); ++i) if( cluster.contains(_instance.getPoint(i)) )
            iloColumn = iloColumn.and(cplex.column(_binding[i], 1));
        
        iloColumn = iloColumn.and(cplex.column(_numberOfClusters, -1));
        
        // Create the variable and store it
        IloNumVar var = cplex.numVar(iloColumn, 0, 1, "x" + _columns.size());
        cplex.add(var);
        
        _variables.put(var, cluster);
    }

    // Gets the solution from the master problem. Returns all non-zero valued columns from the master problem
    public Map<Cluster, Double> getSolution()
    {
        Map<Cluster, Double> ret = new HashMap<Cluster, Double>();
        try
        {
        	for(IloNumVar var: _variables.keySet())
        	{
        		double value = cplex.getValue(var);
        		if( value > 0.01 )
        			ret.put(_variables.get(var), value);
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        System.out.println("Returning solution with " + ret.size() + " clusters");
        return ret;
    }
    
    public boolean isIntegerSolution()
    {
        try
        {
        	for(IloNumVar var: _variables.keySet())
        	{
        		double value = cplex.getValue(var);
        		if( Math.abs(value - (int)value) > 0.01 )
        			return false;
            }
        }
        catch (IloException e)
        {
            e.printStackTrace();
        }
        
        return true;
    }
    
    public double getObjValue()
    {
    	try
    	{
    		return cplex.getObjValue();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	return Double.MAX_VALUE;
    }
    
    public boolean isFeasible()
    {
    	try
    	{
    		return cplex.getStatus() == IloCplex.Status.Optimal || cplex.getStatus() == IloCplex.Status.Feasible;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
    	return false;
    }

    // Closes the master problem
    public void close()
    {
        cplex.end();
    }

    // Listen to branching decisions
    public void performBranching(BranchingDecision bd)
    {
    	System.out.println("Master: Perform branching " + bd);
    	_branchings.add(bd);
    	
    	// TODO: Update the model instead of rebuilding it!
        this.close();
        this.buildModel();
    }

    // Undo branching decisions during backtracking in the Branch-and-Price tree
    public void reverseBranching(BranchingDecision bd)
    {
    	System.out.println("Master: Reverse branching " + bd);
    	_branchings.remove(bd);
    }
    
    public Instance getInstance()
    {
    	return _instance;
    }
    
    public ArrayList<Cluster> getColumns()
    {
    	return _columns;
    }
}