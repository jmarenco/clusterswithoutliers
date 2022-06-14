package interfaz;

public class ArgMap
{
	private String[] args;

	/**
	 * Initialize the ArgMap with command line arguments.
	 * 
	 * @param args
	 */
	public ArgMap(String[] args)
	{
		this.args = args;
	}

	/**
	 * Returns the value associated with the given argument, i.e., the 
	 * string immediately following the given text.
	 * 
	 * Returns null if the argument is not found.
	 * 
	 * @param arg
	 * @return
	 */
	public String getArg(String arg) 
	{
		return getArg(arg, 1);
	}

	/**
	 * Returns the string at n positions after the given argument.
	 * 
	 * Returns null if the argument is not found.
	 * @param arg
	 * @param n
	 * @return
	 */
	public String getArg(String arg, int n) 
	{
		int p = findArg(arg);
		if (p >= 0)
			return args[p+n]; 

		return null;
	}
	

	/**
	 * Returns the value associated with the given argument, i.e., the 
	 * string immediately following the given text.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param defValue
	 * @return
	 */
	public double doubleArg(String arg, double defValue) 
	{
		return doubleArg(arg, 1, defValue);
	}
	
	/**
	 * Returns the string at n positions after the given argument.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param n
	 * @param defValue
	 * @return
	 */
	public double doubleArg(String arg, int n, double defValue) 
	{
		String val = getArg(arg, n);
		if (val != null)
			return Double.parseDouble(val); 

		return defValue;
	}

	/**
	 * Returns the value associated with the given argument, i.e., the 
	 * string immediately following the given text.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param defValue
	 * @return
	 */
	public String stringArg(String arg, String defValue) 
	{
		return stringArg(arg, 1, defValue);
	}
	
	/**
	 * Returns the string at n positions after the given argument.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param n
	 * @param defValue
	 * @return
	 */
	public String stringArg(String arg, int n, String defValue) 
	{
		String val = getArg(arg, n);
		if (val != null)
			return val; 

		return defValue;
	}

	/**
	 * Returns the value associated with the given argument, i.e., the 
	 * string immediately following the given text.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param defValue
	 * @return
	 */
	public int intArg(String arg, int defValue) 
	{
		return intArg(arg, 1, defValue);
	}
	
	/**
	 * Returns the string at n positions after the given argument.
	 * 
	 * If the argument is not found, returns the given defValue.
	 * 
	 * @param arg
	 * @param n
	 * @param defValue
	 * @return
	 */
	public int intArg(String arg, int n, int defValue) 
	{
		String val = getArg(arg, n);
		if (val != null)
			return Integer.parseInt(val); 

		return defValue;
	}

	/**
	 * Indicates if a given argument exists.
	 * 
	 * @param arg
	 * @return
	 */
	public boolean containsArg(String arg) 
	{
		return findArg(arg) >= 0;
	}
	
	private int findArg(String arg)
	{
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equals(arg))
				return i;
		}

		return -1;
	}


}
