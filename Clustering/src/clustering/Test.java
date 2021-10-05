package clustering;

import ilog.concert.IloException;

public class Test
{
	public static void main(String[] args) throws IloException
	{
		if( args.length != 10 && args.length != 11 )
		{
			System.out.println("Parameters: dimension points clusters outliers dispersion seed cutRounds skipFactor cutAndBranch maxTime [symmBreak]");
			return;
		}
		
		int dimension = Integer.parseInt(args[0]);
		int points = Integer.parseInt(args[1]);
		int clusters = Integer.parseInt(args[2]);
		int outliers = Integer.parseInt(args[3]);
		double dispersion = Double.parseDouble(args[4]);
		int seed = Integer.parseInt(args[5]);

		int cutRounds = Integer.parseInt(args[6]);
		int skipFactor = Integer.parseInt(args[7]);
		boolean cutAndBranch  = Integer.parseInt(args[8]) == 1;
		int maxTime = Integer.parseInt(args[9]);
		int symmBreak = args.length > 10 ? Integer.parseInt(args[10]) : 0;
		
		Instance instance = RandomInstance.generate(dimension, points, clusters, outliers, dispersion, seed);
		solve(instance, cutRounds, skipFactor, cutAndBranch, maxTime, symmBreak);
		
//		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.4, 111);
//		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.1, 106);
//		Instance instance = RandomInstance.generate(2, 30, 5, 3, 0.1, 106);
//		Instance instance = RandomInstance.generate(2, 18, 5, 3, 0.05, 106);
//		Instance instance = tostInstance();
		
//		for(int points = 10; points <= 100; points += 5)
//		for(int i=0; i<10; ++i)
//			solve(RandomInstance.generate(2, points, 5, 3, 0.05, 106));
	}
	
	private static void solve(Instance instance) throws IloException
	{
		instance.positivize();
//		instance.print();
		
//		new Viewer(instance, null);

		solve(instance, 0, 0, false, 60, 0);

		for(int rounds = 1; rounds <= 20; ++rounds)
			solve(instance, rounds, 0, false, 60, 0);

		solve(instance, 1000, 0, false, 60, 0);

		for(int rounds = 1; rounds <= 20; ++rounds)
			solve(instance, rounds, 0, true, 60, 0);

		solve(instance, 1000, 0, true, 60, 0);

		for(int skip = 0; skip <= 20; ++skip)
			solve(instance, 1, skip, false, 60, 0);

//		new Viewer(instance, solution);
	}
	
	private static Solution solve(Instance instance, int cutRounds, int skipFactor, boolean cutAndBranch, int maxTime, int symmBreak) throws IloException
	{
		RectangularModel.setVerbose(false);
		RectangularModel.showSummary(true);
		
		if( symmBreak == 1 )
			RectangularModel.setSymmetryBreaking(RectangularModel.SymmetryBreaking.Size);

		if( symmBreak == 2 )
			RectangularModel.setSymmetryBreaking(RectangularModel.SymmetryBreaking.IndexSum);

		Separator.setActive(cutRounds > 0);
		Separator.setMaxRounds(cutRounds);
		Separator.setSkipFactor(skipFactor);
		Separator.setCutAndBranch(cutAndBranch);

		RectangularModel model = new RectangularModel(instance);
	
		model.setMaxTime(maxTime);
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
