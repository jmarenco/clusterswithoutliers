package branchandprice;

import java.util.ArrayList;

import general.Cluster;

public class Node
{
	private Node _parent;
	private BranchingDecision _branchingDecision;
	private ArrayList<Cluster> _newColumns;
	
	public Node()
	{
		_newColumns = new ArrayList<Cluster>();
	}

	public Node(Node parent, BranchingDecision branchingDecision)
	{
		_parent = parent;
		_branchingDecision = branchingDecision;
		_newColumns = new ArrayList<Cluster>();
	}
	
	public Node getParent()
	{
		return _parent;
	}
	
	public BranchingDecision getBranchingDecision()
	{
		return _branchingDecision;
	}
	
	public ArrayList<Cluster> getNewColumns()
	{
		return _newColumns;
	}
	
	public void addColumn(Cluster cluster)
	{
		if( _newColumns.contains(cluster) )
			throw new RuntimeException("Duplicated column added to node! " + cluster);
		
		_newColumns.add(cluster);
	}
	
	public void addColumns(ArrayList<Cluster> clusters)
	{
		for(Cluster cluster: clusters)
			addColumn(cluster);
	}
	
	public ArrayList<Node> pathToRoot()
	{
		ArrayList<Node> ret = new ArrayList<Node>();
		Node current = this;
		
		while( current != null )
		{
			ret.add(current);
			current = current.getParent();
		}
		
		return ret;
	}
}
