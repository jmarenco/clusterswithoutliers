package branchandprice;

import java.util.ArrayList;

public class Node
{
	private int _id;
	private int _height;
	private Node _parent;
	private BranchingDecision _branchingDecision;
	
	public Node(int id)
	{
		_id = id;
		_height = 0;
	}

	public Node(int id, Node parent, BranchingDecision branchingDecision)
	{
		_id = id;
		_height = parent.getHeight() + 1;
		_parent = parent;
		_branchingDecision = branchingDecision;
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
