package colgen;

import java.util.ArrayList;
import java.util.List;

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
		List<Cluster> nuevos = new ArrayList<Cluster>();
		nuevos.add(RectangularCluster.rectangularWithAllPoints(_instance));
		
		while( !nuevos.isEmpty() )
		{
			clusters.addAll(nuevos);
		
			MasterCovering master = new MasterCovering(_instance, clusters);
			master.solve(false);
			
//			Population population = new Population(_instance, master);
//			population.execute();
//			nuevo = population.bestIndividual().fitness() > 0.01 ? population.bestIndividual().asCluster() : null;
			
			RectangularGenerator generator = new RectangularGenerator(_instance, master);
			nuevos = generator.solve();

			for (Cluster nuevo : nuevos)
			{
				System.out.print("It: " + clusters.size() + " | ");
				System.out.print("Obj: " + String.format("%1$,6.2f", master.getObjective()) + " | ");
				System.out.print("Rc: " + (nuevos != null ? String.format("%1$,6.2f", master.reducedCost(nuevo)) : "      ") + " | ");
				System.out.print("Clus: " + nuevo);
				System.out.println();
			}
		}
		
		System.out.println();

		MasterCovering master = new MasterCovering(_instance, clusters);
		master.solve(true);
		
		System.out.println("Obj = " + master.getObjective());
		System.out.println();

		for(int i=0; i<clusters.size(); ++i) if (master.getPrimal(i) > 0.5)
			System.out.println("x[" + i + "] = " + master.getPrimal(i) + "  " + clusters.get(i) + " -> " + clusters.get(i).objective());

		return new Solution(master);
	}
}
