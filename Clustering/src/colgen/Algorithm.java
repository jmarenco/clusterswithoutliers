package colgen;

import java.util.ArrayList;

import general.Cluster;
import general.Instance;
import general.RectangularCluster;
import general.Solution;
import ilog.concert.IloException;

public class Algorithm
{
	private Instance _instance;
	
	public Algorithm(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution run() throws IloException
	{
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		Cluster nuevo = RectangularCluster.rectangularWithAllPoints(_instance);
		
		while( nuevo != null )
		{
			clusters.add(nuevo);
		
			MasterCovering master = new MasterCovering(_instance, clusters);
			master.solve(false);
			
//			Population population = new Population(_instance, master);
//			population.execute();
//			nuevo = population.bestIndividual().fitness() > 0.01 ? population.bestIndividual().asCluster() : null;
			
			RectangularGenerator generator = new RectangularGenerator(_instance, master);
			nuevo = generator.solve();
			
			System.out.print("It: " + clusters.size() + " | ");
			System.out.print("Obj: " + String.format("%1$,6.2f", master.getObjective()) + " | ");
			System.out.print("Rc: " + (nuevo != null ? String.format("%1$,6.2f", master.reducedCost(nuevo)) : "      ") + " | ");
			System.out.print("Clus: " + nuevo);
			System.out.println();
		}
		
		System.out.println();

		MasterCovering master = new MasterCovering(_instance, clusters);
		master.solve(true);
		
		System.out.println("Obj = " + master.getObjective());
		System.out.println();

		for(int i=0; i<clusters.size(); ++i)
			System.out.println("x[" + i + "] = " + master.getPrimal(i) + "  " + clusters.get(i) + " -> " + clusters.get(i).objective());

		return new Solution(master);
	}
}
