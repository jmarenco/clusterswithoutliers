package interfaz;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import general.Instance;
import general.Point;

public class InstanceReader
{
	public static Instance readFromDatafile(String fileName)
	{
		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) 
		{
            String line = br.readLine();
            
            if (line == null)
            	return null;

            String[] values = line.split("\t");
            if (values.length < 4)
            	return null;

            int points = Integer.parseInt(values[0]);
            int dim = Integer.parseInt(values[1]);
            int clusters = Integer.parseInt(values[2]);
            int outliers = Integer.parseInt(values[3]);
            
    		Instance instance = new Instance(clusters, outliers);

    		int i = 0;
            while ((line = br.readLine()) != null) 
            {
                values = line.split("\t");
                Point p = new Point(i++, dim);
                
                int j = 0;
                for (String value : values) 
                {
                    value = value.trim(); // Remove leading and trailing whitespace
                    if (isNumeric(value)) 
                    {
                    	p.set(j++, Double.parseDouble(value));
                    } 
                }
                
                instance.add(p);
            }
            
            return instance;
        } 
		catch (IOException e) 
		{
            e.printStackTrace();
            return null;
        }
    }

    // Utility method to check if a string is numeric
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
