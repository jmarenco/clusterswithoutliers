package interfaz;

import colgen.Algorithm;
import general.Instance;
import general.Point;
import general.RandomInstance;
import ilog.concert.IloException;
import popModel.POPModel;
import standardModel.RectangularModel;
import standardModel.Separator;

public class Test
{
	public static void main(String[] args) throws IloException
	{
		if( args.length > 6 && args[6].equals("sm"))
			solveStandard(args);
		else if( args.length > 6 && args[6].equals("pop"))
			solvePop(args);
		else if( args.length > 6 && args[6].equals("cg"))
			solveColGen(args);
		else
			showUsage();
	}
	
	private static void solveStandard(String[] args) throws IloException
	{
		if( args.length != 11 && args.length != 12 )
		{
			showUsage();
			return;
		}

		int cutRounds = Integer.parseInt(args[7]);
		int skipFactor = Integer.parseInt(args[8]);
		boolean cutAndBranch  = Integer.parseInt(args[9]) == 1;
		int maxTime = Integer.parseInt(args[10]);
		int symmBreak = args.length > 11 ? Integer.parseInt(args[11]) : 0;
		
		Instance instance = constructInstance(args);

		RectangularModel.setVerbose(false);
		RectangularModel.showSummary(true);

		Separator.setActive(cutRounds > 0);
		Separator.setMaxRounds(cutRounds);
		Separator.setSkipFactor(skipFactor);
		Separator.setCutAndBranch(cutAndBranch);

		if( symmBreak == 1 )
			RectangularModel.setSymmetryBreaking(RectangularModel.SymmetryBreaking.Size);

		if( symmBreak == 2 )
			RectangularModel.setSymmetryBreaking(RectangularModel.SymmetryBreaking.IndexSum);

		if( symmBreak == 3 )
			RectangularModel.setSymmetryBreaking(RectangularModel.SymmetryBreaking.OrderedStart);

		RectangularModel model = new RectangularModel(instance);

		model.setMaxTime(maxTime);
		model.setStrongBinding(false);
		model.solve();
	}
	
	private static void solvePop(String[] args) throws IloException
	{
		if( args.length != 8 )
		{
			showUsage();
			return;
		}

		int maxTime = Integer.parseInt(args[7]);
		
		Instance instance = constructInstance(args);
		POPModel model = new POPModel(instance);
		
		model.setMaxTime(maxTime);
		model.solve();
	}
	
	private static void solveColGen(String[] args) throws IloException
	{
		if( args.length != 7 )
		{
			showUsage();
			return;
		}
		
		Instance instance = constructInstance(args);
		Algorithm algorithm = new Algorithm(instance);
		algorithm.run();
	}
	
	private static void showUsage()
	{
		System.out.println("Standard model: dimension points clusters outliers dispersion seed 'sm' cutRounds skipFactor cutAndBranch maxTime [symmBreak]");
		System.out.println("Pop model: dimension points clusters outliers dispersion seed 'pop' maxTime");
		System.out.println("Column generation: dimension points clusters outliers dispersion seed 'cg'");
	}
	
	private static Instance constructInstance(String[] args)
	{
		int dimension = Integer.parseInt(args[0]);
		int points = Integer.parseInt(args[1]);
		int clusters = Integer.parseInt(args[2]);
		int outliers = Integer.parseInt(args[3]);
		double dispersion = Double.parseDouble(args[4]);
		int seed = Integer.parseInt(args[5]);
		
		return RandomInstance.generate(dimension, points, clusters, outliers, dispersion, seed);
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
