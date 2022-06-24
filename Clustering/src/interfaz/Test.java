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
		ArgMap argmap = new ArgMap(args);
		
		if (argmap.containsArg("-?"))
		{
			showUsage();
			return;
		}
		
		String model = argmap.stringArg("-m", "");

		if(model.equals("sm"))
			solveStandard(args);
		else if(model.equals("pop"))
			solvePop(args);
		else if(model.equals("cg"))
			solveColGen(args);
		else
			showUsage();
	}
	
	private static void solveStandard(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);

		int cutRounds = argmap.intArg("-cr", 0);
		int skipFactor = argmap.intArg("-sf", 0);
		boolean cutAndBranch  = argmap.intArg("-cb", 0) == 1;
		int maxTime = argmap.intArg("-tl", 300);
		int symmBreak = argmap.intArg("-symm", 0);

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
		ArgMap argmap = new ArgMap(args);

		POPModel.setVerbose(true);
		POPModel.showSummary(true);

		int maxTime = argmap.intArg("-tl", 300);
		
		Instance instance = constructInstance(args);
		POPModel model = new POPModel(instance);
		
		model.setMaxTime(maxTime);
		model.solve();
	}
	
	private static void solveColGen(String[] args) throws IloException
	{
		Instance instance = constructInstance(args);
		Algorithm algorithm = new Algorithm(instance);
		algorithm.run();
	}
	
	private static void showUsage()
	{
		System.out.println("Available configuration options: ");
		System.out.println("    -m [sm|pop|cg]           Model to use [def:sm]");
		System.out.println("    -d <n>                   Dimension for the instance [def: 2]");
		System.out.println("    -n <n>                   Number of points [def: 10]");
		System.out.println("    -c <n>                   Number of clusters [def: 3]");
		System.out.println("    -o <n>                   Max number of outliers [def: 2]");
		System.out.println("    -disp <f>                Dispersion [def: 0.5]");
		System.out.println("    -s <n>                   Seed for the random generator [def: 0]");
		System.out.println("    -cr <n>                  Cutting rounds for sm model [def: 0]");
		System.out.println("    -sf <n>                  Skip factor for sm model [def: 0]");
		System.out.println("    -cb [1|0]                Use cut and branch on sm model [def: 0]");
		System.out.println("    -tl <n>                  Timelimit [def: 300]");
		System.out.println("    -symm <n>                Symmetry-breaking constraints [def: 0]");
		System.out.println("    -?                       Displays this help");
		System.out.println();
	}
	
	private static Instance constructInstance(String[] args)
	{
		ArgMap argmap = new ArgMap(args);
		
		int dimension = argmap.intArg("-d", 2);
		int points = argmap.intArg("-n", 10);
		int clusters = argmap.intArg("-c", 3);
		int outliers = argmap.intArg("-o", 2);
		double dispersion = argmap.doubleArg("disp", 0.5);
		int seed = argmap.intArg("-s", 0);
		
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
