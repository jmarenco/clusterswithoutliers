package clustering;

import java.util.ArrayList;
import java.util.Scanner;

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
//		clusters.add(Cluster.fromArray(instance, 0, 1, 2, 3, 4, 5, 6));
//		clusters.add(Cluster.fromArray(instance, 3, 4, 5));
//		clusters.add(Cluster.fromArray(instance, 1, 2, 3, 4));
//		clusters.add(Cluster.fromArray(instance, 3, 4, 5, 6));
//		clusters.add(Cluster.fromArray(instance, 1, 2, 4, 5));
		
		Cluster nuevo = Cluster.fromArray(instance, 0, 1, 2, 3, 4, 5, 6);
		
		while( nuevo != null )
		{
			clusters.add(nuevo);
		
			Master master = new Master(instance, clusters);
			master.solve();
	
			System.out.println("Obj = " + master.getObjective());
			System.out.println();
			
			for(int i=0; i<clusters.size(); ++i)
				System.out.println("x[" + i + "] = " + master.getPrimal(i) + "  " + clusters.get(i) + " -> " + clusters.get(i).objective());
	
			System.out.println();
			for(int i=0; i<instance.getPoints() + 2; ++i)
				System.out.println("l[" + i + "] = " + master.getDual(i));
			
			HeuristicGenerator generator = new HeuristicGenerator(instance, master);
			nuevo = generator.solve();
			
			new Scanner(System.in).nextLine();
		}
	}
}
