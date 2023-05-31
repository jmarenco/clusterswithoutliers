package branchandprice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import general.Cluster;
import general.Instance;

public class Solver
{
	private Instance _instance;
	private Master _master;
	private Pricing _pricing;
	private Branching _branching;
	
	private long _timeLimit;
	private long _start;
	private double _ub;
	
	private ArrayList<Node> _nodes;
	private ArrayList<Node> _openNodes;
	private ArrayList<Cluster> _incumbent;
	private Map<Node, Double> _dualBound;
	
	public Solver(Instance instance, long timeLimit)
	{
		_instance = instance;
		_timeLimit = timeLimit;
	}
	
	public void solve()
	{
		// Initializes components
		_master = new Master(_instance);
		_pricing = new Pricing(_master);
		_branching = new Branching(_instance);
		_nodes = new ArrayList<Node>();
		_openNodes = new ArrayList<Node>();
		_dualBound = new HashMap<Node, Double>();
		_start = System.currentTimeMillis();
		
		// Initializes variables
		_master.addFeasibleColumns();
		_incumbent = null;
		_ub = Double.MAX_VALUE;
		
		// Creates root node
		Node root = new Node();
		Node last = null;
		root.addColumns(_master.getColumns());
		
		_nodes.add(root);
		_openNodes.add(root);
		
		// Main loop
		while( _openNodes.size() > 0 && elapsedTime() < _timeLimit )
		{
			Node current = nextNode();
			updateSubproblems(last, current);
			
			boolean newColumns = true;
			while( newColumns == true )
			{
//				_master.buildModel();
				_master.solve(remainingTime());
				_pricing.updateObjective();

				List<Cluster> added = _pricing.generateColumns(remainingTime());
				for(Cluster cluster: added)
					_master.addColumn(cluster);

				newColumns = added.size() > 0;
			}
			
			if( _master.isIntegerSolution() == true )
			{
				System.out.println("Integer solution!");

				if( _master.getObjValue() < _ub )
				{
					_incumbent = new ArrayList<Cluster>(_master.getSolution().keySet());
					_ub = _master.getObjValue();
					
					System.out.println("Incumbent updated! Obj = " + _master.getObjValue());
				}
			}
			else if( _master.isFeasible() == true && _master.getObjValue() < _ub )
			{
				System.out.println("Fractional solution - Branching ...");
				for(BranchingDecision bd: _branching.getBranches(_master.getSolution()))
				{
					Node node = new Node(current, bd);

					_nodes.add(node);
					_openNodes.add(node);
					_dualBound.put(node, _master.getObjValue());
					
					System.out.println(" - Branch created: " + bd);
				}
			}
			else
				System.out.println("Node fathomed!");
			
			_openNodes.remove(current);
			System.out.println("LB: " + getDualBound() + ", UB: " + _ub + " - " + _nodes.size() + " nodes, " + _openNodes.size() + " open nodes");
		}
	}
	
	// Node selection rule
	private Node nextNode()
	{
		return _openNodes.get(_openNodes.size() - 1);
	}
	
	// Updates branching rules in master and pricer
	private void updateSubproblems(Node last, Node current)
	{
		if( last == null )
			return;
		
		ArrayList<Node> fromLast = last.pathToRoot();
		ArrayList<Node> fromCurrent = current.pathToRoot();

		int i = 0;
		while( fromCurrent.contains(fromLast.get(i)) == false )
		{
			_master.reverseBranching(fromLast.get(i).getBranchingDecision());
			_pricing.reverseBranching(fromLast.get(i).getBranchingDecision());
			
			++i;
		}
		
		int j = fromCurrent.indexOf(fromLast.get(i)) - 1;
		while( j >= 0 )
		{
			_master.performBranching(fromCurrent.get(j).getBranchingDecision());
			_pricing.performBranching(fromCurrent.get(j).getBranchingDecision());

			--j;
		}
	}
	
	public double getDualBound()
	{
		return _openNodes.stream().mapToDouble(n -> _dualBound.get(n)).min().orElse(0);
	}
	
	public double elapsedTime()
	{
		return (System.currentTimeMillis() - _start) / 1000.0;
	}
	
	public long remainingTime()
	{
		return _timeLimit - (System.currentTimeMillis() - _start) / 1000;
	}
	
	public ArrayList<Cluster> getSolution()
	{
		return _incumbent;
	}
}
