package branchandprice;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

/**
 * Definition of the pricing problem. Since there's only 1 pricing problem in the cutting stock,
 * we can simply extend the pricing problem included in the framework with no further modifications.
 */
public final class PricingProblem extends AbstractPricingProblem<CuttingStock> {

	public PricingProblem(CuttingStock modelData, String name) {
		super(modelData, name);
	}

}