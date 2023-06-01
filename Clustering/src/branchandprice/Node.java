package branchandprice;

import java.util.ArrayList;

public class Node
{
	private int _id;
	private int _height;
	private Node _parent;
	private BranchingDecision _branchingDecision;
	private ArrayList<Column> _newColumns;
	
	public Node(int id)
	{
		_id = id;
		_height = 0;
		_newColumns = new ArrayList<Column>();
	}

	public Node(int id, Node parent, BranchingDecision branchingDecision)
	{
		_id = id;
		_height = parent.getHeight() + 1;
		_parent = parent;
		_branchingDecision = branchingDecision;
		_newColumns = new ArrayList<Column>();
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getHeight()
	{
		return _height;
	}

	public Node getParent()
	{
		return _parent;
	}
	
	public BranchingDecision getBranchingDecision()
	{
		return _branchingDecision;
	}
	
	public ArrayList<Column> getNewColumns()
	{
		return _newColumns;
	}
	
	public void addColumn(Column column)
	{
		if( _newColumns.stream().anyMatch(c -> !c.isArtificial() && c.getCluster().equals(column.getCluster())) )
			throw new RuntimeException("Duplicated column added to node! " + column.getCluster());
		
		_newColumns.add(column);
	}
	
	public void addColumns(ArrayList<Column> columns)
	{
		for(Column column: columns)
			addColumn(column);
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
