package clustering;

import ilog.concert.IloException;

public class Test
{
	public static void main(String[] args) throws IloException
	{
//		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.4, 111);
		Instance instance = RandomInstance.generate(2, 50, 5, 3, 0.1, 106);
		new Viewer(instance, null);
		
//		Algorithm algorithm = new Algorithm(instance);
//		Solution solution = algorithm.run();
		
		RectangularModel model = new RectangularModel(instance);
		model.setMaxTime(60);
		model.setStrongBinding(false);
		Solution solution = model.solve();

		new Viewer(instance, solution);
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
}
