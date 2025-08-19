package ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.getNonMaintenanceSections;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Slf4j
public final class FindPossibleChainsMaintenanceStage {

    private FindPossibleChainsMaintenanceStage() {
    }

    public static List<Pair<Integer, Integer>> getChains(List<RotaziunSection> sections, IpSolverParams params, ChainingParams chainingParams) {

        List<Pair<Integer, Integer>> possibleChains = new ArrayList<>();

        possibleChains.addAll(getUserDefinedRequiredSectionChains(params));
        possibleChains.addAll(getChainsFromPreviousSolution(params, chainingParams));

        int percentile = ChainingParams.MAINTENANCE_CHAIN_OPTIONS_PERCENTILE[params.getPrecisionLevel()];

        possibleChains.addAll(getChainsFromAllMaintenanceWindows(sections, params, chainingParams, percentile));
        possibleChains.addAll(getChainsToAllMaintenanceWindows(sections, params, chainingParams, percentile));

        possibleChains.addAll(getLargeGapChainingOptions(params, chainingParams, percentile));
        log.info("Count Variables: " + possibleChains.size());

        return possibleChains;
    }

    private static List<Pair<Integer, Integer>> getUserDefinedRequiredSectionChains(IpSolverParams params) {
        return params.getRequiredSectionChains().stream().map(chain -> parseChainToIdxPair(params, chain.getFirst(), chain.getSecond())).toList();
    }

    // do not add user-defined required chains twice
    private static List<Pair<Integer, Integer>> getChainsFromPreviousSolution(IpSolverParams params, ChainingParams chainingParams) {
        return params.getStageParams().getFixedChainMap().entrySet().stream().filter(chain ->
            !chainingParams.getSectionsWithOutgoingFixedChain().contains(chain.getKey())).map(chain ->
            parseChainToIdxPair(params, chain.getKey(), chain.getValue())).toList();
    }

    private static List<Pair<Integer, Integer>> getChainsFromAllMaintenanceWindows(List<RotaziunSection> sections, IpSolverParams params, ChainingParams chainingParams, int percentile) {

        List<RotaziunSection> nonMaintenanceSections = getNonMaintenanceSections(sections, params);
        List<Pair<Integer,Integer>> possibleChains = new ArrayList<>();

        for (RotaziunSection maintenanceWindow : params.getMaintenanceWindows()) {
            if(!chainingParams.getSectionsWithOutgoingFixedChain().contains(maintenanceWindow)){
                List<RotaziunSection> bestToSections = FindPossibleChains.getBestAllowedChainingOptionsForFromSection(maintenanceWindow,nonMaintenanceSections,params,chainingParams,percentile);
                possibleChains.addAll(bestToSections.stream().map(s -> parseChainToIdxPair(params, maintenanceWindow, s)).toList());
            }
        }
        return possibleChains;
    }

    private static List<Pair<Integer, Integer>> getChainsToAllMaintenanceWindows(List<RotaziunSection> sections, IpSolverParams params, ChainingParams chainingParams, int percentile) {

        List<RotaziunSection> nonMaintenanceSections = getNonMaintenanceSections(sections, params);
        List<Pair<Integer,Integer>> possibleChains = new ArrayList<>();

        for (RotaziunSection maintenanceWindow : params.getMaintenanceWindows()) {
            if(!chainingParams.getSectionsWithIncomingFixedChain().contains(maintenanceWindow)) {
                List<RotaziunSection> bestFromSections = FindPossibleChains.getBestAllowedChainingOptionsForToSection(nonMaintenanceSections, maintenanceWindow, params, chainingParams, percentile);
                possibleChains.addAll(bestFromSections.stream().map(s -> parseChainToIdxPair(params, s, maintenanceWindow)).toList());
            }
        }
        return possibleChains;
    }

    private static List<Pair<Integer, Integer>> getLargeGapChainingOptions(IpSolverParams params, ChainingParams chainingParams, int percentile) {

        List<Pair<Integer,Integer>> possibleChains = new ArrayList<>();

        for (RotaziunSection startOfGapSection : chainingParams.getStartOfGapSections()) {
            if(!chainingParams.getSectionsWithOutgoingFixedChain().contains(startOfGapSection)){
                List<RotaziunSection> bestToSections = FindPossibleChains.getBestAllowedChainingOptionsForFromSection(startOfGapSection,chainingParams.getEndOfGapSections(),params,chainingParams,percentile);
                bestToSections = bestToSections.stream().filter(toSection -> isChainNew(params.getStageParams().getFixedChainMap().get(startOfGapSection),toSection)).toList();
                possibleChains.addAll(bestToSections.stream().map(s -> parseChainToIdxPair(params, startOfGapSection, s)).toList());
            }
        }

        return possibleChains;
    }

    private static Pair<Integer, Integer> parseChainToIdxPair(IpSolverParams params, RotaziunSection fromSection, RotaziunSection toSection) {
        return new Pair<>(params.getSectionIdxMap().get(fromSection), params.getSectionIdxMap().get(toSection));
    }

    private static boolean isChainNew(RotaziunSection oldToSection, RotaziunSection newToSection) {
        return !Objects.equals(oldToSection.getId(), newToSection.getId());
    }

}
