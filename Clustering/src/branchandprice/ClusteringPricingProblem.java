package branchandprice;

import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblem;

// Define the pricing problem
public final class ClusteringPricingProblem extends AbstractPricingProblem<InputData>
{
    // Create a new Pricing Problem
    public ClusteringPricingProblem(InputData dataModel, String name)
    {
        super(dataModel, name);
    }
}