package ch.sbb.simba.backend.mikado.solver.ip.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.isNoCouplingOrDecouplingHappening;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getIdleTimeWithoutMinTurnTime;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.determineProblemComplexity;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.MAX_TIME_DIFFERENCE_FOR_FIXED_LINK;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import java.util.Map;
import java.util.stream.Collectors;

public final class StageTwo {

    private StageTwo() {
    }

    public static StageParams getStageTwoParams(IpSolverParams params, Map<RotaziunSection,RotaziunSection> sectionChainMap) {

        Map<RotaziunSection,RotaziunSection> fixedChains = getFixedChainsForStageTwo(params, sectionChainMap);

        return StageParams.builder()
            .stage(2)
            .fixedChainMap(fixedChains)
            .problemComplexity(determineProblemComplexity(params))
            .onlyOneBlock(params.isOnlyOneBlock())
            .withCouplingDecoupling(params.isWithCouplingDecoupling())
            .withSidings(params.isWithSidings())
            .withMaintenance(false)
            .build();

    }

    private static Map<RotaziunSection,RotaziunSection> getFixedChainsForStageTwo(IpSolverParams params, Map<RotaziunSection,RotaziunSection> sectionChainMap) {
        return sectionChainMap.entrySet().stream().filter(entry ->
                shouldChainBeFixedForStamm(params, entry.getKey(), entry.getValue()) || shouldChainBeFixedForEnforcement(params, entry.getKey(), entry.getValue(), sectionChainMap))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static boolean shouldChainBeFixedForStamm(IpSolverParams params, RotaziunSection fromSection, RotaziunSection toSection) {
        return fromSection.getIsStamm() && toSection.getIsStamm() &&
            getIdleTimeWithoutMinTurnTime(fromSection, toSection, params) < getMaxTimeForFixedLink(params);
    }

    private static boolean shouldChainBeFixedForEnforcement(IpSolverParams params, RotaziunSection fromSection, RotaziunSection toSection, Map<RotaziunSection, RotaziunSection> sectionChainMap) {
        return !fromSection.getIsStamm() && !toSection.getIsStamm() &&
            getIdleTimeWithoutMinTurnTime(fromSection, toSection, params) < getMaxTimeForFixedLink(params) &&
            isNoCouplingOrDecouplingHappening(fromSection, toSection, params, sectionChainMap);
    }

    private static int getMaxTimeForFixedLink(IpSolverParams params) {
        if(params.getPrecisionLevel() == 0){
            return MAX_TIME_DIFFERENCE_FOR_FIXED_LINK;
        }
        return (int) (MAX_TIME_DIFFERENCE_FOR_FIXED_LINK * 0.7);
    }

}
