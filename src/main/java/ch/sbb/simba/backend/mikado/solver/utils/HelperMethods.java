package ch.sbb.simba.backend.mikado.solver.utils;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;

import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunStation;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class HelperMethods {

    private HelperMethods() {
    }

    public static Integer mapToPositiveTimeDifference(Integer delta) {
        return delta < 0 ? delta + DAY_IN_SECONDS : delta;
    }

    public static int getKey(Pair<Integer,Integer> x) {
        return x.getFirst() * numOfSections + x.getSecond();
    }

    public static int getCost(Integer duration) {
        return (int) Math.ceil(duration * IpSolverParams.EMPTY_TRIP_COST_PER_SECOND) + IpSolverParams.EMPTY_TRIP_COST_CONSTANT;
    }

    public static boolean isCouplingDecouplingPossibleAtStation(RotaziunStation station, IpSolverParams params) {
        return !params.getProhibitedCouplingDecouplingStationIds().contains(station.getId());
    }

    public static Map<RotaziunSection,RotaziunSection> getChainsFromSolution(List<RotaziunSection> sections, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        Map<RotaziunSection,RotaziunSection> sectionChainMap = new HashMap<>();
        for(Pair<Integer,Integer> p : possibleChains){
            if(v.xMap.get(getKey(p)).solutionValue() == 1){
                sectionChainMap.put(sections.get(p.getFirst()), sections.get(p.getSecond()));
            }
        }
        return sectionChainMap;
    }

    // check wether the corresponding stamm sections are also chained together
    public static boolean isNoCouplingOrDecouplingHappening(RotaziunSection enforcementFrom, RotaziunSection enforcementTo, IpSolverParams params,
        Map<RotaziunSection, RotaziunSection> sectionChainMap) {

        RotaziunSection stammFrom = params.getEnforcementStammPairMap().get(enforcementFrom);
        RotaziunSection stammTo = params.getEnforcementStammPairMap().get(enforcementTo);
        return stammFrom != null && stammTo != null && Objects.equals(sectionChainMap.get(stammFrom).getId(), stammTo.getId());
    }

    public static List<Pair<RotaziunSection, RotaziunSection>> parseMapToList(Map<RotaziunSection, RotaziunSection> map) {
        return map.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    public static Set<Pair<RotaziunSection, RotaziunSection>> parseChainsFromIdsToObjects(List<RotaziunSection> sections, Set<Pair<Long, Long>> chainsId) {
        Map<Long, RotaziunSection> sectionMap = sections.stream().collect(Collectors.toMap(RotaziunSection::getId, s -> s));
        return chainsId.stream().map(chain -> new Pair<>(sectionMap.get(chain.getFirst()), sectionMap.get(chain.getSecond()))).collect(Collectors.toSet());
    }

    public static List<RotaziunSection> prepenadMaintenanceSections(List<RotaziunSection> commercialSections, List<RotaziunSection> maintenanceSections) {
        List<RotaziunSection> sections = new ArrayList<>();
        sections.addAll(maintenanceSections);
        sections.addAll(commercialSections);
        return sections;
    }

}