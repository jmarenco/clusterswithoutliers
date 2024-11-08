package branchandprice;

import general.Instance;
import general.Solution;
import incremental.BlackBoxClusteringSolver;

public class Wrapper implements BlackBoxClusteringSolver
{
	private Solver _solver = null;

	@Override
	public void setMaxTime(int tl)
	{
		Solver.setTimeLimit(tl);
	}

	@Override
	public Solution solve(Instance ins) throws Exception {
		Solution trivial_solution = Solution.withAllPoints(ins);
		return solve(ins, trivial_solution);
	}

	
	@Override
	public Solution solve(Instance ins, Solution initial_solution) throws Exception
	{
		_solver = new Solver(ins);
		_solver.solve();

		return new Solution(_solver.getSolution());
	}
	
	
	
	@Override
	public void closeSolver()
	{
	}

	@Override
	public double getLastLB()
	{
		return _solver.getDualBound();
	}

	@Override
	public int getNSolutions() throws Exception
	{
		return _solver.getFoundSolutions().size();
	}

	@Override
	public double getObjValueOfSolutionN(int i) throws Exception
	{
		return this.getSolutionNumber(i).calcObjVal();
	}

	@Override
	public Solution getSolutionNumber(int i) throws Exception
	{
		return _solver.getFoundSolutions().get(this.getNSolutions() - i - 1);
	}

	@Override
	public String getSolverName()
	{
		return "B&P";
	}
}
