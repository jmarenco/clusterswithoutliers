package branchandprice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.HashMap;

import general.Cluster;
import general.Instance;

public class Solver
{
	private Instance _instance;
	private Master _master;
	private Pricing _pricing;
	private Pricing _rootPricing;
	private Branching _branching;
	
	private long _start;
	private double _ub;
	
	private ArrayList<Node> _nodes;
	private ArrayList<Node> _openNodes;
	private ArrayList<Cluster> _incumbent;
	private Map<Node, Double> _dualBound;
	
	public static enum Pricer { ZLR, FLZ, Heuristic, None };
	
	private static long _timeLimit = 3600;
	private static boolean _verbose = true;
	private static boolean _summary = false;
	private static Pricer _pricer = Pricer.ZLR;
	private static Pricer _rootPricer = Pricer.None;

	public Solver(Instance instance)
	{
		_instance = instance;
	}
	
	public void solve()
	{
		// Initializes components
		_master = new MasterWithRebuild(_instance);
		_branching = new Branching(_instance);
		_nodes = new ArrayList<Node>();
		_openNodes = new ArrayList<Node>();
		_dualBound = new HashMap<Node, Double>();
		_start = System.currentTimeMillis();
		
		if( _pricer == Pricer.ZLR )
			_pricing = new PricingZLRModel(_master);
		else
			_pricing = new PricingFLZModel(_master);
		
		if( _rootPricer == Pricer.Heuristic )
			_rootPricing = new PricingHeuristic(_master);

		// Initializes variables
		_master.addFeasibleColumns();
		_incumbent = null;
		_ub = Math.max(1000, 2 * IntStream.range(0, _instance.getDimension()).mapToDouble(t -> _instance.max(t) - _instance.min(t)).sum());
		
		// Creates root node
		Node root = new Node(0);
		Node last = null;
		
		_nodes.add(root);
		_openNodes.add(root);
		
		// Main loop
		while( _openNodes.size() > 0 && elapsedTime() < _timeLimit )
		{
			Node current = nextNode();
//			System.out.println("Solving node " + current.getId() + ", " + current.getBranchingDecision() + (current.getParent() != null ? " - Parent: Node " + current.getParent().getId() : ""));

//			Node aux = current;
//			while( aux != null )
//			{
//				System.out.println(aux.getBranchingDecision());
//				aux = aux.getParent();
//			}
			
			updateSubproblems(last, current);
			
			boolean incumbentUpdated = false;
			boolean newColumns = true;
			int addedColumns = 0;
			
			while( newColumns == true )
			{
//				_master.buildModel();
				_master.solve(remainingTime());
				newColumns = false;

				if( _master.isOptimal() == true )
				{
					List<Cluster> added = null;

					if( _nodes.size() == 1 && _rootPricing != null )
					{
						_rootPricing.updateObjective();
						added = _rootPricing.generateColumns(remainingTime());
					}
					
					if( added == null || added.size() == 0 )
					{
						_pricing.updateObjective();
						added = _pricing.generateColumns(remainingTime());
					}
	
					for(Cluster cluster: added)
						_master.addColumn(cluster);
	
					newColumns = added.size() > 0;
					addedColumns += added.size();
				}
			}
			
			if( _master.isIntegerSolution() == true )
			{
//				System.out.println("Integer solution!");

				if( _master.getObjValue() < _ub )
				{
					_incumbent = new ArrayList<Cluster>(_master.getSolution().keySet());
					_ub = _master.getObjValue();

					incumbentUpdated = true;
//					System.out.println("Incumbent updated! Obj = " + _master.getObjValue());
				}
			}
			else if( _master.isFeasible() == true && _master.getObjValue() < _ub )
			{
//				System.out.println("Fractional solution - Branching ...");
				for(BranchingDecision bd: _branching.getBranches(_master.getSolution(), _master.getOutliers()))
				{
					Node node = new Node(_nodes.size(), current, bd);

					_nodes.add(node);
					_openNodes.add(node);
					_dualBound.put(node, _master.getObjValue());
					
//					System.out.println(" - Branch created: " + bd);
				}
			}
//			else
//				System.out.println("Node fathomed!");
			
			_openNodes.remove(current);
			last = current;
			
			if( _verbose == true )
				showStatistics(current, addedColumns, incumbentUpdated);
		}

		if( _summary == true )
			showSummary();
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

//		System.out.println("From last (" + last.getId() + "):");
//		for(Node n: fromLast)
//			System.out.println(n.getId());
//		System.out.println("From current (" + current.getId() + "):");
//		for(Node n: fromCurrent)
//			System.out.println(n.getId());

		int i = 0;
		while( fromCurrent.contains(fromLast.get(i)) == false )
		{
			_master.reverseBranching(fromLast.get(i).getBranchingDecision());
			_pricing.reverseBranching(fromLast.get(i).getBranchingDecision());
			
			++i;
		}
		
//		System.out.println("Int: " + fromLast.get(i).getId());

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
	
	private void showStatistics(Node current, int addedColumns, boolean incumbentUpdated)
	{
		double dualBound = getDualBound();
		double gap = _ub > 0 ? 100 * (_ub - dualBound) / _ub : 100;
		
		System.out.print("LB: " + String.format("%8.4f", dualBound));
		System.out.print(incumbentUpdated ? "*| " : " | ");
		System.out.print("UB: " + String.format("%9.4f", _ub));
		System.out.print(" (" + String.format("%5.2f", gap) + "%) | ");
		System.out.print("Nodes: " + _nodes.size() + " | ");
		System.out.print("Open: " + _openNodes.size() + " | ");
		System.out.print(String.format("%7.2f", elapsedTime()) + " sec | ");
		System.out.print("Cols: " + _master.getColumns().size());
		
		if( current != null )
		{
			System.out.print(" (" + addedColumns + " new) | ");
			System.out.print("Cur: " + current.getId() + ", H: " + current.getHeight() + " - ");
			System.out.print(current.getBranchingDecision());
		}
		
		System.out.println();
	}
	
	private void showSummary()
	{
		double dualBound = getDualBound();
		double gap = _ub > 0 ? 100 * (_ub - dualBound) / _ub : 100;
		double pricingTime = _pricing.getSolvingTime() + (_rootPricing != null ? _rootPricing.getSolvingTime() : 0);
		
		System.out.print(_instance.getName() + " | B&P | ");
		System.out.print(_openNodes.size() == 0 ? "Optimal | " : "Feasible | ");
		System.out.print("Obj: " + String.format("%6.4f", _ub) + " | ");
		System.out.print(String.format("%6.2f", elapsedTime()) + " sec. | ");
		System.out.print(_nodes.size() + " nodes | ");
		System.out.print(String.format("%6.2f", gap) + " % | ");
		System.out.print(" 0 cuts | ");
		System.out.print(_master.getColumns().size() + " cols | ");
		System.out.print("M: " + String.format("%6.2f", _master.getSolvingTime()) + " sec. | ");
		System.out.print("P: " + String.format("%6.2f", pricingTime) + " sec. | ");
		System.out.print(PricingZLRModel.stopWhenNegative() ? "NegPr | " : "      | ");
		System.out.print("MT: " + _timeLimit + " | ");
		System.out.print("Pr: " + (_pricer == Pricer.ZLR ? "ZLR" : "FLZ")  + " | ");
		System.out.print(_rootPricer == Pricer.Heuristic ? "HC: " + _rootPricing.getGeneratedColumns() + " | " : " | ");
		System.out.println();
	}	

	public ArrayList<Cluster> getSolution()
	{
		return _incumbent;
	}
	
	public static void setTimeLimit(long timeLimit)
	{
		_timeLimit = timeLimit;
	}
	
	public static void setVerbose(boolean verbose)
	{
		_verbose = verbose;
	}
	
	public static void showSummary(boolean summary)
	{
		_summary = summary;
	}
	
	public static void setPricer(Pricer pricer)
	{
		_pricer = pricer;
	}
	
	public static void setRootPricer(boolean heuristic)
	{
		_rootPricer = heuristic ? Pricer.Heuristic : Pricer.None;
	}
}
