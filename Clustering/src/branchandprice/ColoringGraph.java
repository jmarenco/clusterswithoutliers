package branchandprice;

import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

/**
 * Class defining a graph coloring instance
 */
public final class ColoringGraph implements ModelInterface
{
	private String _file;
	
	public ColoringGraph(String file)
	{
		_file = file;
	}
	
    public int getNrVertices()
    {
        return 1;
    }

    @Override
    public String getName()
    {
        return _file;
    }
}