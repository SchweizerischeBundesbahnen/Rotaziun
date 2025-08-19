package ch.sbb.simba.backend.mikado.solver.ip.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.determineProblemComplexity;

import java.util.HashMap;

public final class StageOne {

    private StageOne() {
    }

    public static StageParams getStageOneParams(IpSolverParams params) {

        if(params.isTwoStages()){
            return StageParams.builder()
                .stage(1)
                .fixedChainMap(new HashMap<>())
                .problemComplexity(params.getPrecisionLevel() >= 1 ? 1 : 0)
                .onlyOneBlock(params.getPrecisionLevel() >= 1)
                .withCouplingDecoupling(false)
                .withSidings(false)
                .withMaintenance(false)
                .build();
        }

        return StageParams.builder()
            .stage(1)
            .fixedChainMap(new HashMap<>())
            .problemComplexity(determineProblemComplexity(params))
            .onlyOneBlock(params.isOnlyOneBlock())
            .withCouplingDecoupling(params.isWithCouplingDecoupling())
            .withSidings(params.isWithSidings())
            .withMaintenance(false)
            .build();

    }
}
