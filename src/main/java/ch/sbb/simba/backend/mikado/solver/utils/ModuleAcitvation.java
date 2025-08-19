package ch.sbb.simba.backend.mikado.solver.utils;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isChainAtCapacityRestrictedStation;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isSidingHappening;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunInputParams;
import java.util.List;

public final class ModuleAcitvation {

    private ModuleAcitvation() {
    }

    public static boolean solveWithTwoStages(IpSolverParams params, List<RotaziunSection> sections) {
        if(sections.size() < IpSolverParams.MIN_NUMBER_OF_SECTIONS_FOR_TWO_STAGE_APPROACH) {
            return false;
        }
        if(params.getPrecisionLevel() == 0){
            return determineProblemComplexity(params) > 0;
        }
        return determineProblemComplexity(params) > 1;
    }

    public static int determineProblemComplexity(IpSolverParams params) {
        return (params.isOnlyOneBlock() ? 1 : 0) + (params.isWithCouplingDecoupling() ? 1 : 0) + (params.isWithSidings() ? 1 : 0);
    }

    public static boolean isCouplingDecouplingModuleNeeded(RotaziunInputParams input) {
        return input.getCostForCouplingDecoupling() > 0 ||
            input.getMinTimeForDecoupling() > 0 ||
            input.getMinTimeForCoupling() > 0 ||
            !input.getProhibitedCouplingDecouplingStationIds().isEmpty();
    }

    public static boolean areCouplingVariablesNeeded(IpSolverParams params) {
        return params.getMinTimeForCoupling() != 0 || !params.getProhibitedCouplingDecouplingStationIds().isEmpty();
    }

    public static boolean isSidingModuleNeeded(RotaziunInputParams input) {
        return !input.getSidingCapacityMap().isEmpty();
    }

    public static boolean isMaintenaceModuleNeeded(List<RotaziunSection> maintenanceSections) {
        return !maintenanceSections.isEmpty();
    }

    public static void validateModulUsage(IpSolverParams params) {

        couplingDecouplingModuleRequiresOneBlock(params);
        sidingModuleRequiresOneBlock(params);
        maintenanceMpduleRequiresOneBlock(params);

    }

    private static void couplingDecouplingModuleRequiresOneBlock(IpSolverParams params) {
        if(params.isWithCouplingDecoupling() && !params.isOnlyOneBlock()){
            throw new IllegalArgumentException("CouplingDecoupling Module requires OneBlock");
        }
    }

    private static void sidingModuleRequiresOneBlock(IpSolverParams params) {
        if(params.isWithSidings() && !params.isOnlyOneBlock()){
            throw new IllegalArgumentException("Siding Module requires OneBlock");
        }
    }

    private static void maintenanceMpduleRequiresOneBlock(IpSolverParams params) {
        if(params.isWithMaintenance() && !params.isOnlyOneBlock()){
            throw new IllegalArgumentException("Maintenance Module requires OneBlock");
        }
    }

    public static boolean isSidingVariableNeeded(IpSolverParams params, List<RotaziunSection> sections, Pair<Integer, Integer> p) {
        return isSidingHappening(sections.get(p.getFirst()), sections.get(p.getSecond()), params)
            && isChainAtCapacityRestrictedStation(sections.get(p.getFirst()), sections.get(p.getSecond()), params);
    }

    public static boolean isSidingVariableNeeded(IpSolverParams params, RotaziunSection fromSection, RotaziunSection toSection) {
        return isSidingHappening(fromSection, toSection, params)
            && isChainAtCapacityRestrictedStation(fromSection, toSection, params);
    }

}
