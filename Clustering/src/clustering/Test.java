package clustering;

import java.util.ArrayList;
import ilog.concert.IloException;

public class Test
{
	public static void main(String[] args) throws IloException
	{
		Instance instance = new Instance(2, 1);
		instance.add(Point.fromVector(1, 1.0, 1.0));
		instance.add(Point.fromVector(2, 1.0, 2.0));
		instance.add(Point.fromVector(3, 1.5, 1.5));
		instance.add(Point.fromVector(4, 4.0, 4.0));
		instance.add(Point.fromVector(5, 5.0, 3.0));
		instance.add(Point.fromVector(6, 5.0, 4.0));
		instance.add(Point.fromVector(7, 2.0, 4.0));
		
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		Cluster nuevo = Cluster.withAllPoints(instance);
		
		while( nuevo != null )
		{
			clusters.add(nuevo);
		
			Master master = new Master(instance, clusters);
			master.solve(false);
			
			HeuristicGenerator generator = new HeuristicGenerator(instance, master);
			nuevo = generator.solve();
			
			System.out.print("It: " + clusters.size() + " | ");
			System.out.print("Obj: " + String.format("%1$,6.2f", master.getObjective()) + " | ");
			System.out.print("Rc: " + (nuevo != null ? String.format("%1$,6.2f", generator.reducedCost(nuevo)) : "      ") + " | ");
			System.out.print("Clus: " + nuevo);
			System.out.println();
		}
		
		System.out.println();

		Master master = new Master(instance, clusters);
		master.solve(true);

		System.out.println("Obj = " + master.getObjective());
		System.out.println();
		
		for(int i=0; i<clusters.size(); ++i)
			System.out.println("x[" + i + "] = " + master.getPrimal(i) + "  " + clusters.get(i) + " -> " + clusters.get(i).objective());
	}
}
