package colgen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import branchandprice.Solver;
import general.Cluster;
import general.Instance;
import general.Solution;
import ilog.concert.IloException;

public class HalfRelaxedAlgorithm
{
	private Instance _instance;
	private static boolean _summary = true;
	
	public HalfRelaxedAlgorithm(Instance instance)
	{
		_instance = instance;
	}
	
	public Solution run() throws IloException
	{
		long start = System.currentTimeMillis();

		Solver.setVerbose(false);
		Solver.showSummary(false);
		Solver.setBrancher(Solver.Brancher.OnlyOutliers);
		
		Solver solver = new Solver(_instance);
        solver.solve();
        
        List<Cluster> clusters = solver.getMaster().getColumns().stream().map(c -> c.getCluster()).collect(Collectors.toList());
        
		MasterCovering master = new MasterCovering(_instance, new ArrayList<Cluster>(clusters));
		master.solve(true);
		
		double time = (System.currentTimeMillis() - start) / 1000.0;

		if( _summary == true)
		{
			System.out.print(_instance.getName() + " | CGHR | ");
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
}
