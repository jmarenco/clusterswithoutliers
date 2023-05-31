package jorlib;

import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

import general.Instance;

// Instance to be solved
public final class InputData implements ModelInterface
{
	private Instance _instance;
	
	public InputData(Instance instance)
	{
		_instance = instance;
	}
    
    public Instance getInstance()
    {
    	return _instance;
    }

    @Override
    public String getName()
    {
        return _instance.getName();
    }
}