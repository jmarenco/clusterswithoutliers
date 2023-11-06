package branchandprice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import general.Cluster;
import general.Instance;
import general.NearestNeighbors;
import general.Point;

public class PricingHeuristicWithBranching extends Pricing
{
	// Input data
	private Master _master;
	private Instance _instance;
	private NearestNeighbors _neighbors;
	private Random _random;
	private int p; // Points
	private int d; // dimension
	private double[] dualCosts;

	// Branching rules
	private boolean[] _outliers;
	private List<Double>[][] _branchBoundLL, _branchBoundLU, _branchBoundRL, _branchBoundRU; 
												// L/R = left/right; L/U = lower/upper.
												// Lists are sorted in increasing way
	// Statistics
	private double _solvingTime = 0;
	private int _generatedColumns = 0;
	
	// Creates a new solver instance for a particular pricing problem
    @SuppressWarnings("unchecked")
	public PricingHeuristicWithBranching(Master master)
    {
    	_master = master;
        _instance = master.getInstance();
        _neighbors = new NearestNeighbors(_instance);
        _random = new Random(0);
        
		p = _instance.getPoints();
		d = _instance.getDimension();
		
		_outliers = new boolean[p];
		_branchBoundLL = new ArrayList[p][d];
		_branchBoundLU = new ArrayList[p][d];
		_branchBoundRL = new ArrayList[p][d];
		_branchBoundRU = new ArrayList[p][d];
		for (int i = 0; i < p; i++)
			for (int t = 0; t < d; t++)
			{
				_branchBoundLL[i][t] = new ArrayList<Double>();
				_branchBoundLU[i][t] = new ArrayList<Double>();
				_branchBoundRL[i][t] = new ArrayList<Double>();
				_branchBoundRU[i][t] = new ArrayList<Double>();
			}
    }
	
	// Main method for solving the pricing problem
    public List<Cluster> generateColumns(double timeLimit)
    {
        List<Cluster> ret = new ArrayList<>();
        double start = System.currentTimeMillis();
        
        for(int i=0; i<p; ++i) if(!_outliers[i] && _random.nextDouble() < 0.2)
        {
        	Cluster cluster = constructFrom(i);
        	if( cluster != null && ret.contains(cluster) == false )
        		ret.add(cluster);
        }

        _solvingTime += (System.currentTimeMillis() - start) / 1000.0;
        _generatedColumns += ret.size();
        
        return ret;
    }
    
    private Cluster constructFrom(int origin)
    {
    	double pointDuals = dualCosts[origin];
    	double currentDual = -pointDuals;
    	Point point = _instance.getPoint(origin);

    	// The current box for the cluster
    	double[] currentL = new double[d];
    	double[] currentR = new double[d];
    	for (int t = 0; t < d; t++)
    	{
    		currentL[t] = point.get(t);
    		currentR[t] = point.get(t);
    	}

    	Cluster ret = Cluster.fromArray(_instance, origin);
    	int[] neighbors = _neighbors.get(origin);
    	
    	for(int i=0; i<neighbors.length; ++i) 
    	{
    		int j = neighbors[i];
    		
    		Point point_j = _instance.getPoint(j);
    		if (!_outliers[j] && compatibleWithBox(point_j, j, currentL, currentR))
    		{
	    		ret.add(point_j);
	    		
	    		double newDual = ret.totalSpan() - pointDuals - dualCosts[j];
	    		if( currentDual > newDual )
	    		{
	    			// Expand the current box (if necessary)
	    			double[] newCurrentL = new double[d];
	    			double[] newCurrentR = new double[d];
	    	    	for (int t = 0; t < d; t++)
	    	    	{
	    	    		newCurrentL[t] = Math.min(currentL[t], point_j.get(t));
	    	    		newCurrentR[t] = Math.max(currentR[t], point_j.get(t));
	    	    	}

	    	    	// Have to check if the new box is compatible with all the rest of the points!
	    	    	boolean is_ok = true;
	    	    	for (int k =0; k < p; k++) if (k != j)
	    	    	{
	    	    		Point point_k = _instance.getPoint(k);
		    	    	if (ret.contains(point_k) && !compatibleWithBox(point_k, k, newCurrentL, newCurrentR))
		    	    	{
		    	    		is_ok = false;
		    	    		break;
		    	    	}	    		
	    	    	}

	    	    	if (is_ok)
	    	    	{
		    	    	currentDual = newDual;
		    			pointDuals += dualCosts[j];
		    	    	currentL = newCurrentL;
		    	    	currentR = newCurrentR;
	    	    	}
	    	    	else
		    			ret.remove(point_j);
	    	    }
	    		else
	    			ret.remove(point_j);
    		}
    	}
    	
    	// If no other points where added (i.e., the cluster is a singleton) it still may be incompatible (and this has not been checked in the code)
    	if (ret.size() == 1 && !compatibleWithBox(point, origin, currentL, currentR))
    		return null;
    	
    	
    	// Finally, we add to the cluster every point which is free to add 
    	for (int i = 0; i < p; i++) if (i != origin && !_outliers[i])
    	{
    		Point point_i = _instance.getPoint(i);
	    	if (!ret.contains(point_i) && compatibleWithBox(point_i, i, currentL, currentR))
	    	{
	    		boolean is_inside = true;
	    		// we check now if point is inside the box
	    		for (int t = 0; t < d; t++)
	    		{
	    			if (point_i.get(t) <= currentL[t] || point_i.get(t) >= currentR[t])
	    			{
	    				is_inside = false;
	    				break;
	    			}
	    		}
	    		
	    		if (is_inside) // Then it's free to add it!
	    		{
	    			ret.add(point_i);
	    			pointDuals += dualCosts[i];
	    		}
	    	}
    	}
    	
    	return ret.totalSpan() - pointDuals + dualCosts[p] < -0.01 ? ret : null;
    }

    /**
     * Checks if the current box is compatible for the point given the current branching rules.
     * @param point
     * @param currentL
     * @param currentR
     * @return
     */
    private boolean compatibleWithBox(Point point, int i, double[] currentL, double[] currentR) 
    {
    	for (int t = 0; t < d; t++)
    	{
    		// currentL[t] cannot be LOWER than the MAXIMUM threshold for this point on this dimension
    		if (!_branchBoundLL[i][t].isEmpty() && currentL[t] < _branchBoundLL[i][t].get(_branchBoundLL[i][t].size() - 1))
    			return false;

    		// currentL[t] cannot be HIGHER than the MINIMUM threshold for this point on this dimension
    		if (!_branchBoundLU[i][t].isEmpty() && currentL[t] > _branchBoundLU[i][t].get(0))
    			return false;

    		// currentR[t] cannot be LOWER than the MAXIMUM threshold for this point on this dimension
    		if (!_branchBoundRL[i][t].isEmpty() && currentR[t] < _branchBoundRL[i][t].get(_branchBoundRL[i][t].size() - 1))
    			return false;

    		// currentR[t] cannot be HIGHER than the MINIMUM threshold for this point on this dimension
    		if (!_branchBoundRU[i][t].isEmpty() && currentR[t] > _branchBoundRU[i][t].get(0))
    			return false;
    	}

    	return true;
	}

	// Update the objective function of the pricing problem with the new dual information. The dual values are stored in the pricing problem.
    public void updateObjective()
    {
    	dualCosts = _master.getDuals();
    }

    // Close the pricing problem
    public void close()
    {
    }

    // Listen to branching decisions. The pricing problem is changed by the branching decisions.
    public void performBranching(BranchingDecision sc)
    {
       	if( sc instanceof BranchOnSide )
    		performBranchingOnSide((BranchOnSide)sc);
    	
    	if (sc instanceof BranchOnOutlier )
    		performBranchingOnOutlier((BranchOnOutlier)sc);
    }
    
    private void performBranchingOnSide(BranchOnSide sc)
    {
//    	System.out.println("Pricing: Perform branching " + sc);
        	
    	List<Double> bounds = null;
    	
     	if( sc.appliesToMinSide() && sc.isLowerBound())
       		bounds = _branchBoundLL[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMinSide() && sc.isUpperBound())
       		bounds = _branchBoundLU[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMaxSide() && sc.isLowerBound())
       		bounds = _branchBoundRL[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMaxSide() && sc.isUpperBound())
       		bounds = _branchBoundRU[sc.getPoint()][sc.getDimension()];
        	
     	addInOrder(bounds, sc.getThreshold());
    }
    
    private void addInOrder(List<Double> list, double value)
    {
    	int index = Collections.binarySearch(list, value);
        if (index < 0) 
        {
            index = -index - 1;
        }
        list.add(index, value);    	
    }


    private void performBranchingOnOutlier(BranchOnOutlier sc)
    {
//        System.out.println("Pricing: Perform branching " + sc);
    	if (sc.mustBeOutlier())
    		_outliers[sc.getPoint()] = true;
    }
        
    // When the Branch-and-Price algorithm backtracks, branching decisions are reversed.
    public void reverseBranching(BranchingDecision sc)
    {
       	if( sc instanceof BranchOnSide )
    		reverseBranchingOnSide((BranchOnSide)sc);
    	
    	if (sc instanceof BranchOnOutlier )
    		reverseBranchingOnOutlier((BranchOnOutlier)sc);
    }
    
    private void reverseBranchingOnSide(BranchOnSide sc)
    {
//        	System.out.println("Pricing: Perform branching " + sc);
        	
    	List<Double> bounds = null;
    	
     	if( sc.appliesToMinSide() && sc.isLowerBound())
       		bounds = _branchBoundLL[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMinSide() && sc.isUpperBound())
       		bounds = _branchBoundLU[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMaxSide() && sc.isLowerBound())
       		bounds = _branchBoundRL[sc.getPoint()][sc.getDimension()];
       	else if( sc.appliesToMaxSide() && sc.isUpperBound())
       		bounds = _branchBoundRU[sc.getPoint()][sc.getDimension()];
        	
     	bounds.remove(sc.getThreshold());
    }
    
    private void reverseBranchingOnOutlier(BranchOnOutlier sc)
    {
//        System.out.println("Pricing: Perform branching " + sc);
    	if (sc.mustBeOutlier())
    		_outliers[sc.getPoint()] = false;
    }

    public double getSolvingTime()
    {
    	return _solvingTime;
    }
    
    public int getGeneratedColumns()
    {
    	return _generatedColumns;
    }
}
