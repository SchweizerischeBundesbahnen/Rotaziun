package ch.sbb.simba.backend.mikado.solver.ip.parameters;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.isMaintenanceSection;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import java.util.Map;
import java.util.stream.Collectors;

public final class MaintenanceStage {

    private MaintenanceStage() {
    }

    public static StageParams getMaintenanceStageParams(IpSolverParams params, Map<RotaziunSection,RotaziunSection> sectionChainMap) {

        Map<RotaziunSection,RotaziunSection> fixedChains = getFixedChainsForMaintenanceStage(sectionChainMap);

        return StageParams.builder()
            .stage(3)
            .fixedChainMap(fixedChains)
            .problemComplexity(1) // not relevant for maintenance stage
            .onlyOneBlock(true)
            .withCouplingDecoupling(params.isWithCouplingDecoupling())
            .withSidings(params.isWithSidings())
            .withMaintenance(true)
            .build();
    }

    // fix all chains excpet the once to and from a maintenance section
    private static Map<RotaziunSection,RotaziunSection> getFixedChainsForMaintenanceStage(Map<RotaziunSection,RotaziunSection> sectionChainMap) {
        return sectionChainMap.entrySet().stream()
            .filter(entry ->
                !isMaintenanceSection(entry.getKey()) && !isMaintenanceSection(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
