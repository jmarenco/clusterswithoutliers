package interfaz;

import colgen.Algorithm;
import general.Instance;
import general.Point;
import general.RandomInstance;
import general.Solution;
import ilog.concert.IloException;
import popModel.POPModel;
import standardModel.RectangularModel;
import standardModel.Separator;
import standardModel.RectangularModel.SymmetryBreaking;

public class Test
{
	public static void main(String[] args) throws IloException
	{
		if( args.length != 11 && args.length != 12 )
		{
			System.out.println("Parameters: method dimension points clusters outliers dispersion seed cutRounds skipFactor cutAndBranch maxTime [symmBreak]");
			System.out.println(" - method: sm = standard model, pop = POP model, cg = column generation");
			return;
		}
		
		Method method = args[0].equals("sm") ? Method.Standard : (args[0].equals("pop") ? Method.POP : Method.ColGen); 
		
		int dimension = Integer.parseInt(args[1]);
		int points = Integer.parseInt(args[2]);
		int clusters = Integer.parseInt(args[3]);
		int outliers = Integer.parseInt(args[4]);
		double dispersion = Double.parseDouble(args[5]);
		int seed = Integer.parseInt(args[6]);

		int cutRounds = Integer.parseInt(args[7]);
		int skipFactor = Integer.parseInt(args[8]);
		boolean cutAndBranch  = Integer.parseInt(args[9]) == 1;
		int maxTime = Integer.parseInt(args[10]);
		int symmBreak = args.length > 11 ? Integer.parseInt(args[11]) : 0;
		
		Instance instance = RandomInstance.generate(dimension, points, clusters, outliers, dispersion, seed);
		solve(instance, method, cutRounds, skipFactor, cutAndBranch, maxTime, symmBreak);
	}
	
	private static enum Method { Standard, POP, ColGen };
	
	private static void solve(Instance instance, Method method) throws IloException
	{
		instance.positivize();
//		instance.print();
		
//		new Viewer(instance, null);

		solve(instance, method, 0, 0, false, 60, 0);

		for(int rounds = 1; rounds <= 20; ++rounds)
			solve(instance, method, rounds, 0, false, 60, 0);

		solve(instance, method, 1000, 0, false, 60, 0);

		for(int rounds = 1; rounds <= 20; ++rounds)
			solve(instance, method, rounds, 0, true, 60, 0);

		solve(instance, method, 1000, 0, true, 60, 0);

		for(int skip = 0; skip <= 20; ++skip)
			solve(instance, method, 1, skip, false, 60, 0);

//		new Viewer(instance, solution);
	}
	
	private static Solution solve(Instance instance, Method method, int cutRounds, int skipFactor, boolean cutAndBranch, int maxTime, int symmBreak) throws IloException
	{
		Solution ret = null;

		if( method == Method.Standard )
		{
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
			
			ret = model.solve();
		}
		
		if( method == Method.POP )
		{
			POPModel model = new POPModel(instance);
			
			model.setMaxTime(maxTime);
			model.setStrongBinding(false);
			
			ret = model.solve();
		}
		
		if( method == Method.ColGen )
		{
			Algorithm algorithm = new Algorithm(instance);
			ret = algorithm.run();
		}
		
		return ret;
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
