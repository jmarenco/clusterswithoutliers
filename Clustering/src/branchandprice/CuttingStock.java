package branchandprice;

import org.jorlib.frameworks.columnGeneration.model.ModelInterface;

/**
 * Define a Cutting Stock problem
 * 
 */
public final class CuttingStock implements ModelInterface{

	public final int nrFinals=4; //Number of different finals
	public final int rollWidth=100; //Width of the raws
	public final int[] finals={45, 36, 31, 14}; //Size of the finals
	public final int[] demandForFinals={97, 610, 395, 211}; //Requested quantity of each final

	@Override
	public String getName() {
		return "CuttingStockExample";
	}
}