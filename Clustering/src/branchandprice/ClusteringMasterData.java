package branchandprice;

import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import java.util.Map;

// Container which stores information coming from the master problem. It contains
// a reference to the cplex model and a reference to the pricing problem
public final class ClusteringMasterData extends MasterData<InputData, PotentialCluster, ClusteringPricingProblem, IloNumVar>
{
    public final IloCplex cplex;

    // Creates a new MasterData object
    public ClusteringMasterData(IloCplex cplex, Map<ClusteringPricingProblem, OrderedBiMap<PotentialCluster, IloNumVar>> varMap)
    {
        super(varMap);
        this.cplex=cplex;
    }
}