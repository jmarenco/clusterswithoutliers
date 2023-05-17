package branchandprice;

import ilog.concert.IloNumVar;
import org.jorlib.frameworks.columnGeneration.master.MasterData;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import java.util.Map;


public final class CuttingStockMasterData extends MasterData<CuttingStock, CuttingPattern, PricingProblem, IloNumVar>{

    public CuttingStockMasterData(Map<PricingProblem, OrderedBiMap<CuttingPattern, IloNumVar>> varMap) {
        super(varMap);
    }
}