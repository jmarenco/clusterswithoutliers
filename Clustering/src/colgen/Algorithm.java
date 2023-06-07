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
	private static boolean _summary = true;
	private static boolean _relaxation = false;
	
	public Algorithm(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution run() throws IloException
	{
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		List<Cluster> nuevos = new ArrayList<Cluster>();
		nuevos.add(RectangularCluster.rectangularWithAllPoints(_instance));
		
		long start = System.currentTimeMillis();

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

//			for (Cluster nuevo : nuevos)
//			{
//				System.out.print("It: " + clusters.size() + " | ");
//				System.out.print("Obj: " + String.format("%1$,6.2f", master.getObjective()) + " | ");
//				System.out.print("Rc: " + (nuevos != null ? String.format("%1$,6.2f", master.reducedCost(nuevo)) : "      ") + " | ");
//				System.out.print("Clus: " + nuevo);
//				System.out.println();
//			}
		}
		
//		System.out.println();

		MasterCovering master = new MasterCovering(_instance, clusters);
		master.solve(!_relaxation);
		
//		System.out.println("Obj = " + master.getObjective());
//		System.out.println();
//
//		for(int i=0; i<clusters.size(); ++i) if (master.getPrimal(i) > 0.5)
//			System.out.println("x[" + i + "] = " + master.getPrimal(i) + "  " + clusters.get(i) + " -> " + clusters.get(i).objective());
		
		double time = (System.currentTimeMillis() - start) / 1000.0;

		if( _summary == true)
		{
			System.out.print(_instance.getName() + (_relaxation ? " | CGR | " : " | CG  | "));
			System.out.print(" | ");
			System.out.print("Obj: " + String.format("%6.4f", master.getObjective()) + " | ");
			System.out.print(String.format("%6.2f", time) + " sec. | ");
			System.out.print(" | ");
			System.out.print(" | ");
			System.out.print(" | ");
			System.out.print(clusters.size() + " cols | ");
			System.out.print("SF: " + 0 + " | ");
			System.out.print("Cut execs: " + 0 + " | ");
			System.out.print("    | ");
			System.out.print("MT: | ");
			System.out.print("SB: |"); 
			System.out.println();
		}
		

		return new Solution(master);
	}
	
	public static void showSummary(boolean value)
	{
		_summary = value;
	}
	
	public static void solveRelaxation(boolean value)
	{
		_relaxation = value;
	}
}
