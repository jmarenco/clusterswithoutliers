package incremental;

import general.Instance;
import general.Solution;

public interface BlackBoxClusteringSolver 
{

	/**
	 * Terminates anything which needs termination inside the solver (if any such thing is needed).
	 */
	void closeSolver();

	/**
	 * Sets a time limit for the next solution process.
	 * @param tl time limit in milliseconds
	 */
	void setMaxTime(int tl);
	
	/**
	 * Solves the given instance and returns the best solution found (if any)
	 * @return
	 */
	Solution solve(Instance ins) throws Exception;

	/**
	 * Returns the best lower bound obtained in the last execution.
	 */
	double getLastLB();

	/**
	 * Returns the number of feasible solutions found in the last execution.
	 */
	int getNSolutions() throws Exception;

	/**
	 * Returns the objective function value of the feasible solution number i from the last execution.
	 */
	double getObjValueOfSolutionN(int i) throws Exception;

	/**
	 * Returns the feasible solution number i from the last execution.
	 */
	Solution getSolutionNumber(int i) throws Exception;

}
