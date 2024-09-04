package interfaz;

import branchandprice.MasterWithRebuild;
import branchandprice.Pricing;
import branchandprice.PricingFLZModel;
import branchandprice.PricingZLRModel;
import branchandprice.Solver;
import colgen.Algorithm;
import colgen.HalfRelaxedAlgorithm;
import general.Instance;
import general.Point;
import general.RandomInstance;
import general.Solution;
import ilog.concert.IloException;
import incremental.BorderPointsManager;
import incremental.EccentricityManager;
import incremental.IncrementalSolver;
import incremental.IncrementalStandardModel;
import kmeans.KMeansSolver;
import popModel.POPModel;
import repModel.RepModel;
import standardModel.LinearSeparator;
import standardModel.LinearSeparatorSparse;
import standardModel.RectangularModel;
import standardModel.Separator;
import standardModel.SquareSeparator;

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

		if(argmap.containsArg("-writeonly"))
			writeInstance(args);
		else if(argmap.containsArg("-showonly"))
			showInstance(args);
		else if(model.equals("sm"))
			solveStandard(args);
		else if(model.equals("pop"))
			solvePop(args);
		else if(model.equals("cg"))
			solveColGen(args);
		else if(model.equals("rep"))
			solveRep(args);
		else if(model.equals("bap"))
			solveBap(args);
		else if(model.equals("kmn"))
			solveKMeans(args);
		else if(model.equals("inc"))
			solveIncremental(args);
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
		double threshold = argmap.doubleArg("-thr", 0.5);
		int sepStrategy = argmap.intArg("-sstr", 0);
		int objective = argmap.intArg("-fobj", 0);
		double lowerLimit = argmap.doubleArg("-llim", 0.1);
		double upperLimit = argmap.doubleArg("-ulim", 0.9);
		double sparsityRatio = argmap.doubleArg("-sprat", 0.25);

		Instance instance = constructInstance(args);

		RectangularModel.setVerbose(argmap.containsArg("-verbose"));
		RectangularModel.showSummary(true);
		RectangularModel.setObjective(objective == 1 ? RectangularModel.Objective.Area : RectangularModel.Objective.Span);
		
		Separator.setActive(cutRounds > 0);
		Separator.setMaxRounds(cutRounds);
		Separator.setSkipFactor(skipFactor);
		Separator.setCutAndBranch(cutAndBranch);
		Separator.setStrategy(sepStrategy);
		LinearSeparator.setThreshold(threshold);
		LinearSeparatorSparse.setThreshold(threshold);
		LinearSeparatorSparse.setLowerLimit(lowerLimit);
		LinearSeparatorSparse.setUpperLimit(upperLimit);
		SquareSeparator.setThreshold(threshold);
		SquareSeparator.setSparsingRatio(sparsityRatio);

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

		POPModel.setVerbose(argmap.containsArg("-verbose"));
		POPModel.showSummary(!argmap.containsArg("-verbose"));

		int maxTime = argmap.intArg("-tl", 300);
		
		Instance instance = constructInstance(args);
		POPModel model = new POPModel(instance);
		
		model.setMaxTime(maxTime);
		model.solve();
	}
	
	private static void solveRep(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);

		RepModel.setVerbose(argmap.containsArg("-verbose"));
		RepModel.showSummary(!argmap.containsArg("-verbose"));

		int maxTime = argmap.intArg("-tl", 300);
		
		Instance instance = constructInstance(args);
		RepModel model = new RepModel(instance);
		
		model.setMaxTime(maxTime);
		model.solve();
	}

	private static void solveColGen(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);
		Instance instance = constructInstance(args);
		
		if( argmap.containsArg("-partialrelaxation") == false )
		{
			Algorithm.solveRelaxation(argmap.containsArg("-relaxation"));
			Algorithm algorithm = new Algorithm(instance);
			algorithm.run();
		}
		else
		{
			HalfRelaxedAlgorithm algorithm = new HalfRelaxedAlgorithm(instance);
			algorithm.run();
		}
	}
	
	private static void solveBap(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);
		Instance instance = constructInstance(args);
		
		PricingZLRModel.stopWhenNegative(argmap.containsArg("-negpr"));
		PricingFLZModel.stopWhenNegative(argmap.containsArg("-negpr"));

		Solver.setTimeLimit(argmap.intArg("-tl", 3600));
		Solver.setVerbose(argmap.containsArg("-verbose"));
		Solver.showSummary(!argmap.containsArg("-verbose"));
		Solver.setRootPricer(argmap.containsArg("-hrp"));
		int pricer_id = argmap.intArg("-pr", 0);
		Solver.setPricer(pricer_id == 0 ? Solver.Pricer.ZLR : (pricer_id == 1 ? Solver.Pricer.FLZ : Solver.Pricer.Heuristic));
		Solver.setBrancher(argmap.containsArg("-rf") ? Solver.Brancher.RyanFoster : Solver.Brancher.Side);
		Pricing.setMaxColsPerPricing(argmap.intArg("-maxcols", 1));
		MasterWithRebuild.setInitialSingletons(argmap.containsArg("-initialsingletons"));
		
		Solver solver = new Solver(instance);
        solver.solve();
	}
	
	private static void solveKMeans(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);
		Instance instance = constructInstance(args);

		KMeansSolver solver = new KMeansSolver(instance);
		
		KMeansSolver.setTimeLimit(argmap.intArg("-tl", 3600));
		KMeansSolver.setVerbose(argmap.containsArg("-verbose"));
		KMeansSolver.showSummary(true);
		
		
		solver.solve();
	}

	private static void solveIncremental(String[] args) throws IloException
	{
		ArgMap argmap = new ArgMap(args);

		int cutRounds = argmap.intArg("-cr", 0);
		int skipFactor = argmap.intArg("-sf", 0);
		boolean cutAndBranch  = argmap.intArg("-cb", 0) == 1;
		int maxTime = argmap.intArg("-tl", 300);
		int symmBreak = argmap.intArg("-symm", 0);
		double threshold = argmap.doubleArg("-thr", 0.5);
		int sepStrategy = argmap.intArg("-sstr", 0);
		int objective = argmap.intArg("-fobj", 0);
		double lowerLimit = argmap.doubleArg("-llim", 0.1);
		double upperLimit = argmap.doubleArg("-ulim", 0.9);

		Instance instance = constructInstance(args);

		IncrementalStandardModel.setVerbose(argmap.containsArg("-verbose"));
		IncrementalStandardModel.showSummary(!argmap.containsArg("-verbose"));
		IncrementalStandardModel.setObjective(objective == 1 ? IncrementalStandardModel.Objective.Area : IncrementalStandardModel.Objective.Span);
		
		Separator.setActive(cutRounds > 0);
		Separator.setMaxRounds(cutRounds);
		Separator.setSkipFactor(skipFactor);
		Separator.setCutAndBranch(cutAndBranch);
		Separator.setStrategy(sepStrategy);
		LinearSeparator.setThreshold(threshold);
		LinearSeparatorSparse.setThreshold(threshold);
		LinearSeparatorSparse.setLowerLimit(lowerLimit);
		LinearSeparatorSparse.setUpperLimit(upperLimit);

		if( symmBreak == 1 )
			IncrementalStandardModel.setSymmetryBreaking(IncrementalStandardModel.SymmetryBreaking.Size);

		if( symmBreak == 2 )
			IncrementalStandardModel.setSymmetryBreaking(IncrementalStandardModel.SymmetryBreaking.IndexSum);

		if( symmBreak == 3 )
			IncrementalStandardModel.setSymmetryBreaking(IncrementalStandardModel.SymmetryBreaking.OrderedStart);

		IncrementalSolver.setMetric(argmap.stringArg("-incmetric", "dist"));
		IncrementalSolver.setShowIntermediateSolutions(argmap.containsArg("-incshow")); 
		
		EccentricityManager.setSumOverDimensions(argmap.stringArg("-eccmode", "MAX").equals("SUM"));
		EccentricityManager.setIncrementStep(argmap.intArg("-incstep", 20));
		EccentricityManager.setMaxDistanceToNeighbour(argmap.doubleArg("-maxdist", 0.2));

		BorderPointsManager.setIncrementStep(argmap.intArg("-incstep", 20));
		BorderPointsManager.setMaxDistanceToNeighbour(argmap.doubleArg("-maxdist", 0.2));

		IncrementalSolver solver = new IncrementalSolver(instance);
//		IncrementalStandardModel solver = new IncrementalStandardModel(instance);
//		solver.setStrongBinding(false);

		solver.setMaxTime(maxTime);
		solver.solve();
	}
	
	private static void writeInstance(String[] args)
	{
		ArgMap argmap = new ArgMap(args);
		Instance instance = constructInstance(args);
		
		InstanceWriter.write(instance, argmap.stringArg("-writeonly", "instance.dat"));
	}

	private static void showInstance(String[] args)
	{
		Instance instance = constructInstance(args);
		new Viewer(instance, new Solution());
	}
	
	private static void showUsage()
	{
		System.out.println("Available configuration options: ");
		System.out.println("    -m [sm|pop|cg|rep|bap]                 Model to use [def:sm]");
		System.out.println("    -d <n>                                 Dimension for the instance [def: 2]");
		System.out.println("    -n <n>                                 Number of points [def: 10]");
		System.out.println("    -c <n>                                 Number of clusters [def: 3]");
		System.out.println("    -o <n>                                 Max number of outliers [def: 2]");
		System.out.println("    -disp <f>                              Dispersion [def: 0.5]");
		System.out.println("    -s <n>                                 Seed for the random generator [def: 0]");
		System.out.println("    -cr <n>                                Cutting rounds for sm model [def: 0]");
		System.out.println("    -sf <n>                                Skip factor for sm model [def: 0]");
		System.out.println("    -cb [0|1]                              Use cut and branch on sm model [def: 0]");
		System.out.println("    -sstr [0-4]                            Separation strategy on sm model [def: 0]");
		System.out.println("    -sprat <f>                             Sparsity ratio in square separator for sm model [def: 0.25]");
		System.out.println("    -fobj [0|1]                            Objective function in sm model [def: 0]");
		System.out.println("    -llim <n>                              Lower limit for sparse separation in sm model [def: 0.1]");
		System.out.println("    -ulim <n>                              Upper limit for sparse separation in sm model [def: 0.9]");
		System.out.println("    -tl <n>                                Timelimit [def: 300]");
		System.out.println("    -symm <n>                              Symmetry-breaking constraints [def: 0]");
		System.out.println("    -thr <f>                               Threshold for adding cuts [def: 0.5]");
		System.out.println("    -pr [0|1|2]                            Pricing strategy in bap model [def: 0]");
		System.out.println("    -maxcols <n>                           Max number of columns per pricing in bap model [def: 1]");
		System.out.println("    -negpr                                 Stop pricing with negative objective in bap model");
		System.out.println("    -hrp                                   Heuristic for pricing at root node");
		System.out.println("    -rf                                    Ryan-Foster branching");
		System.out.println("    -relaxation                            Solve the linear relaxation at the root node (cg model only)");
		System.out.println("    -partialrelaxation                     Solve the partial linear relaxation at the root node (cg model only)");
		System.out.println("    -initialsingletons                     Add singleton columns (bap model only)");
		System.out.println("    -incmetric [none|rand|ecc|dist|bord]   Pricing strategy in bap model [def: dist]");
		System.out.println("    -incshow                               Shows a plot displaying each intermadiate solution of the incremental solver");
		System.out.println("    -eccmode [max|sum]                     Eccentricty mode for global eccentricity [def: MAX]");
		System.out.println("    -incstep <n>                           Max number of points to add on each incremental iteration [def: 20]");
		System.out.println("    -maxdist <f>                           Max distance to neighbours (for incremental resolution) [def: 0.2]");
		System.out.println("    -verbose                               Verbose output");
		System.out.println("    -writeonly <s>                         Does not solve, only writes instance to file <s>");
		System.out.println("    -showonly                              Does not solve, only show a plot with the instance");
		System.out.println("    -ins <s>                               Read the instance from <s>");
		System.out.println("    -?                                     Displays this help");
		System.out.println();
}
	
	private static Instance constructInstance(String[] args)
	{
		ArgMap argmap = new ArgMap(args);
		
		if (argmap.containsArg("-ins"))
		{
			return InstanceReader.readFromDatafile(argmap.getArg("-ins"));
		}
		else // Generate random instance 
		{
			int dimension = argmap.intArg("-d", 2);
			int points = argmap.intArg("-n", 10);
			int clusters = argmap.intArg("-c", 3);
			int outliers = argmap.intArg("-o", 2);
			double dispersion = argmap.doubleArg("-disp", 0.5);
			int seed = argmap.intArg("-s", 0);
			
			return RandomInstance.generate(dimension, points, clusters, outliers, dispersion, seed);
		}
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
	
	public static Instance tustInstance()
	{
		Instance instance = new Instance(2, 1);
		instance.add(Point.fromVector(1, 1.0, 1.0));
		instance.add(Point.fromVector(2, 1.0, 2.0));
		instance.add(Point.fromVector(3, 1.5, 1.5));
		instance.add(Point.fromVector(4, 4.0, 4.0));
		instance.add(Point.fromVector(5, 5.0, 3.0));
		instance.add(Point.fromVector(6, 5.0, 4.0));
		instance.add(Point.fromVector(7, 2.0, 4.0));
		instance.add(Point.fromVector(8, 2.0, 5.0));
		
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
