package interfaz;

import java.io.FileWriter;
import java.io.BufferedWriter;
import general.Instance;
import general.Point;

public class InstanceWriter
{
	private static int _digits = 6;
	
	public static void write(Instance instance, String fileName)
	{
		try
		{
		    BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

		    writer.write(instance.getPoints() + "\t");
		    writer.write(instance.getDimension() + "\t");
		    writer.write(instance.getClusters() + "\t");
		    writer.write(instance.getOutliers() + "\r\n");
		    
		    for(int i=0; i<instance.getPoints(); ++i)
		    {
		    	Point point = instance.getPoint(i);
		    	for(int j=0; j<point.getDimension(); ++j)
		    		writer.write((int)(Math.pow(10, _digits) * point.get(j)) + "\t");
		    	
		    	writer.write("\r\n");
		    }
		    
		    writer.close();
		    System.out.println("Written: " + fileName);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
