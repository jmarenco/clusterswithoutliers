package general;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger 
{
	private static String logfile = "log.txt";
	private static final String tab = "\t";
	
	static public void log(Instance ins, String method, Results res)
	{
		
	    BufferedWriter writer;
		try 
		{
			writer = new BufferedWriter(new FileWriter(logfile, true));
		    writer.write(ins.log_details(tab));
		    writer.write(tab + method);
		    writer.write(tab + res.log_details(tab));
		    writer.write("\n");
		    writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		System.out.print("MyLog\t");
		System.out.print(ins.log_details(tab));
		System.out.print(tab + method);
		System.out.print(tab + res.log_details(tab));
		System.out.print("\n");
	}

	public static void setLogFile(String nfile) 
	{
		logfile = nfile;
	}
}
