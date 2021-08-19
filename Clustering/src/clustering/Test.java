package clustering;

import ilog.concert.IloException;

public class Test
{
	public static void main(String[] args) throws IloException
	{
//		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.4, 111);
//		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.1, 106);
//		Instance instance = RandomInstance.generate(2, 30, 5, 3, 0.1, 106);
		Instance instance = RandomInstance.generate(2, 18, 5, 3, 0.05, 106);
//		Instance instance = tostInstance();
		
		instance.positivize();
//		instance.print();
		
//		new Viewer(instance, null);

		solve(instance, 0, 0, false);
		solve(instance, 1000, 0, false);
		
		for(int rounds = 0; rounds <= 20; ++rounds)
			solve(instance, rounds, 0, false);

		for(int skip = 0; skip <= 20; ++skip)
			solve(instance, 1, skip, false);

//		new Viewer(instance, solution);
	}
	
	private static Solution solve(Instance instance, int cutRounds, int skipFactor, boolean cutAndBranch) throws IloException
	{
		RectangularModel.setVerbose(false);
		RectangularModel.showSummary(true);

		Separator.setActive(cutRounds > 0);
		Separator.setMaxRounds(cutRounds);
		Separator.setSkipFactor(skipFactor);
		Separator.setCutAndBranch(cutAndBranch);

		RectangularModel model = new RectangularModel(instance);
	
		model.setMaxTime(600);
		model.setStrongBinding(false);
		return model.solve();
	}
	
	public static Instance testInstance()
	{
		Instance instance = new Instance(2, 1);
		instance.add(Point.fromVector(1, 1.0, 1.0));
		instance.add(Point.fromVector(2, 1.0, 2.0));
		instance.add(Point.fromVector(3, 1.5, 1.5));
		instance.add(Point.fromVector(4, 4.0, 4.0));
		instance.add(Point.fromVector(5, 5.0, 3.0));
		instance.add(Point.fromVector(6, 5.0, 4.0));
		instance.add(Point.fromVector(7, 2.0, 4.0));
		
		return instance;
	}
	
	public static Instance tostInstance()
	{
		Instance instance = new Instance(4, 2);
		instance.add(Point.fromVector(1, 1.0, 1.0));
		instance.add(Point.fromVector(2, 1.0, 2.0));
		instance.add(Point.fromVector(3, 1.5, 1.5));
		instance.add(Point.fromVector(4, 4.0, 4.0));
		instance.add(Point.fromVector(5, 5.0, 3.0));
		instance.add(Point.fromVector(6, 5.0, 4.0));
		instance.add(Point.fromVector(7, 2.0, 4.0));
		instance.add(Point.fromVector(8, 5.0, 5.0));
		instance.add(Point.fromVector(9, 1.3, 1.2));
		instance.add(Point.fromVector(10, 3.3, 5.2));
		instance.add(Point.fromVector(11, 3.2, 5.2));
		instance.add(Point.fromVector(12, 3.8, 5.4));
		instance.add(Point.fromVector(13, 3.1, 5.7));
		instance.add(Point.fromVector(14, 3.6, 5.5));
		instance.add(Point.fromVector(15, 3.3, 1.2));
		instance.add(Point.fromVector(16, 3.2, 1.2));
		instance.add(Point.fromVector(17, 3.8, 1.4));
		instance.add(Point.fromVector(18, 3.1, 1.7));
		instance.add(Point.fromVector(19, 3.6, 1.5));
		
		return instance;
	}
}
