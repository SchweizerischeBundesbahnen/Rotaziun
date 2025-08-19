package ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.determineSectionType;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.filters.ChainingOptionsFilter;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class FindPossibleChains {

    private FindPossibleChains() {
    }

    public static List<Pair<Integer, Integer>> getChains(List<RotaziunSection> sections, IpSolverParams params, ChainingParams chainingParams) {

        List<Pair<Integer,Integer>> possibleChains = new ArrayList<>();
        for (int i = 0; i < numOfSections; i++) {
            if(chainingParams.getSectionsWithOutgoingFixedChain().contains(sections.get(i))){
                possibleChains.add(new Pair<>(i, params.getSectionIdxMap().get(chainingParams.getFixedChainMap().get(sections.get(i)))));
            } else {
                possibleChains.addAll(getPossibleChainsForGivenSection(i, sections, params, chainingParams));
            }
        }
        return possibleChains;
    }

    private static List<Pair<Integer,Integer>> getPossibleChainsForGivenSection(int fromSectionIdx, List<RotaziunSection> sections, IpSolverParams params, ChainingParams chainingParams) {

        int percentile = chainingParams.getChainOptionsPercentiles().get(determineSectionType(sections.get(fromSectionIdx), chainingParams));
        List<RotaziunSection> bestChainingOptions = getBestAllowedChainingOptionsForFromSection(sections.get(fromSectionIdx), sections, params, chainingParams, percentile);
        return bestChainingOptions.stream().map(s -> new Pair<>(fromSectionIdx, params.getSectionIdxMap().get(s))).toList();

    }

    public static List<RotaziunSection> getBestAllowedChainingOptionsForFromSection(RotaziunSection fromSection, List<RotaziunSection> toSections, IpSolverParams params,
        ChainingParams chainingParams, int percentile){

        List<RotaziunSection> allowedToSections = ChainAnalysis.getAllowedToSections(fromSection, toSections, params, chainingParams);
        Map<RotaziunSection,Integer> valuedChainTimeMap = getValuedChainTimeMap(fromSection,allowedToSections,params);
        return ChainingOptionsFilter.filterByPercentile(valuedChainTimeMap, percentile, params);
    }

    public static List<RotaziunSection> getBestAllowedChainingOptionsForToSection(List<RotaziunSection> fromSections, RotaziunSection toSection, IpSolverParams params,
        ChainingParams chainingParams, int percentile){

        List<RotaziunSection> allowedFromSections = ChainAnalysis.getAllowedFromSections(fromSections, toSection, params, chainingParams);
        Map<RotaziunSection,Integer> valuedChainTimeMap = getValuedChainTimeMap(allowedFromSections,toSection,params);
        return ChainingOptionsFilter.filterByPercentile(valuedChainTimeMap, percentile, params);
    }

    private static Map<RotaziunSection, Integer> getValuedChainTimeMap(RotaziunSection fromSection, List<RotaziunSection> toSections, IpSolverParams params) {
        return toSections.stream().collect(Collectors.toMap(s -> s, s -> ChainAnalysis.computeValuedChainTime(fromSection, s, params)));
    }

    private static Map<RotaziunSection, Integer> getValuedChainTimeMap(List<RotaziunSection> fromSections, RotaziunSection toSection, IpSolverParams params) {
        return fromSections.stream().collect(Collectors.toMap(s -> s, s -> ChainAnalysis.computeValuedChainTime(s, toSection, params)));
    }

}