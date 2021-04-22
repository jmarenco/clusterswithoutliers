package clustering;

import java.util.Random;

public class RandomInstance
{
	public static Instance generate(int dimension, int points, int clusters, int outliers, double dispersion)
	{
		return generate(dimension, points, clusters, outliers, dispersion, 0);
	}
	
	public static Instance generate(int dimension, int points, int clusters, int outliers, double dispersion, int seed)
	{
		Random random = new Random(seed);
		Instance instance = new Instance(clusters, outliers);
		
		Point[] centroids = new Point[clusters];
		
		for(int i=0; i<clusters; ++i)
			centroids[i] = randomPoint(random, 0, dimension, 2);
		
		for(int i=0; i<points-outliers; ++i)
		{
			Point point = randomPoint(random, i+1, dimension, dispersion);
			point.sum( centroids[random.nextInt(clusters)] );
			
			instance.add(point);
		}
		
		for(int i=0; i<outliers; ++i)
			instance.add(randomPoint(random, points-outliers+i+1, dimension, 2));
		
		return instance;
	}
	
	private static Point randomPoint(Random random, int id, int dimension, double range)
	{
		Point ret = new Point(id, dimension);
		
		for(int i=0; i<dimension; ++i)
			ret.set(i, range * random.nextDouble() - range / 2);
		
		return ret;
	}
}
