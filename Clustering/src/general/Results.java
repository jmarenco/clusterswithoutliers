package general;

public class Results 
{
	public Solution sol = null;
	
	public enum Status { OPTIMAL, FEASIBLE, NOSOLUTION };
	public Status sol_status = Status.NOSOLUTION;

	public double lb = 0.0;
	public double ub = Double.MAX_VALUE;

	public double total_time = -1;

	public int nodes = 0;
	public int incremental_iterations = 0;
	public int used_points = 0;
	
	public Results() { }
	
	public Results(Solution s, Status status, double lb, double ub, double time, int nodes, int incr_iterations, int used_points)
	{
		this.sol = s;
		this.sol_status = status;
		this.lb = lb;
		this.ub = ub;
		this.total_time = time;
		this.nodes = nodes;
		this.incremental_iterations = incr_iterations;
		this.used_points = used_points;
	}
	
	
	public void printStats() 
	{
		System.out.println("Overall results: ");
		System.out.println("   Total time:    " + total_time/1000.0 + " seg.");
		System.out.println("   Best solution: " + lb);
		System.out.println("   Best bound:    " + ub);
		System.out.println("   Status:        " + sol_status);
	}

	public String log_details(String tab) 
	{
		return sol_status
				+ tab + lb
				+ tab + ub
				+ tab + total_time
				+ tab + nodes
				+ tab + incremental_iterations
				+ tab + used_points;
	}
}
