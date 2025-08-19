package ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis;

import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.mapToPositiveTimeDifference;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.isMaintenanceSection;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.EMPTY_TRIP_MULTIPLIER;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.STAMM_CHANGE_ADDITION;

import ch.sbb.simba.backend.mikado.solver.utils.RotaziunDurationMap;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunInputParams;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ChainAnalysis {

    private ChainAnalysis() {
    }

    // stilllagerzeit zwischen 2 Fahrten ohne minTurnTime
    public static int getIdleTimeWithoutMinTurnTime(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        int emptyTripDuration = getEmptyTripDuration(fromSection, toSection, params.getDurationMap());
        int arrivalTimeAfterEmptyTrip = (fromSection.getArrival() + params.getMinTurnTime() + emptyTripDuration) % DAY_IN_SECONDS;
        int idleTime =  mapToPositiveTimeDifference(toSection.getDeparture() - arrivalTimeAfterEmptyTrip) + emptyTripDuration;
        assert idleTime > 0;
        return idleTime;
    }

    // stilllagerzeit zwischen 2 Fahrten mit minTurnTime
    public static int getIdleTime(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params){
        return getIdleTimeWithoutMinTurnTime(fromSection,toSection,params) + params.getMinTurnTime();
    }

    public static Integer getEmptyTripDuration(RotaziunSection fromSection, RotaziunSection toSection, RotaziunDurationMap durationMap) {
        if(Objects.equals(fromSection.getToStation().getId(), toSection.getFromStation().getId())){
            return 0;
        }
        return durationMap.getDuration(fromSection.getToStation().getId(), toSection.getFromStation().getId());
    }

    public static boolean isSidingHappening(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        return getIdleTime(fromSection, toSection, params) > params.getMinSidingDuration();
    }

    public static boolean isSidingHappening(Pair<RotaziunSection, RotaziunSection> chain, RotaziunInputParams input, RotaziunDurationMap durationMap) {
        int emptyTripDuration = getEmptyTripDuration(chain.getFirst(), chain.getSecond(), durationMap);
        int arrivalTimeAfterEmptyTrip = (chain.getFirst().getArrival() + input.getMinTurnTime() + emptyTripDuration) % DAY_IN_SECONDS;
        int idleTime =  mapToPositiveTimeDifference(chain.getSecond().getDeparture() - arrivalTimeAfterEmptyTrip) + emptyTripDuration;
        return idleTime > input.getMinSidingDuration();
    }

    public static boolean isChainAtCapacityRestrictedStation(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        return params.getSidingCapacityMap().containsKey(fromSection.getToStation().getId()) || params.getSidingCapacityMap().containsKey(toSection.getFromStation().getId());
    }

    static boolean isChainAllowed(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params, Set<RotaziunSection> sectionsWithIncomingFixedChain) {
        return !sectionsWithIncomingFixedChain.contains(toSection) &&
            !isChainInProhibitedSidingSite(fromSection, toSection, params) &&
            !chainContainsProhibitedEmptyTrip(fromSection, toSection, params);
    }

    public static List<RotaziunSection> getAllowedToSections(RotaziunSection fromSection, List<RotaziunSection> toSections, IpSolverParams params, ChainingParams chainingParams) {
        return toSections.stream().filter(toSection -> isChainAllowed(fromSection,toSection,params,chainingParams.getSectionsWithIncomingFixedChain())).toList();
    }

    public static List<RotaziunSection> getAllowedFromSections(List<RotaziunSection> fromSections, RotaziunSection toSection, IpSolverParams params, ChainingParams chainingParams) {
        return fromSections.stream().filter(fromSection ->
            !chainingParams.getSectionsWithOutgoingFixedChain().contains(fromSection) &&
            isChainAllowed(fromSection,toSection,params,chainingParams.getSectionsWithIncomingFixedChain())).toList();
    }

    public static int isStammChange(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params){
        if(isMaintenanceSection(fromSection)||isMaintenanceSection(toSection)){
            return 0;
        }
        if(getIdleTimeWithoutMinTurnTime(fromSection,toSection,params) > IpSolverParams.STAMM_CHANGE_PENALTY_FREE_PERIOD && toSection.getDeparture() < fromSection.getArrival()){
            return 0;
        }
        return fromSection.getIsStamm() == toSection.getIsStamm() ? 0 : 1;
    }

    public static int isDifferentDebicodes(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params){
        if(isMaintenanceSection(fromSection)||isMaintenanceSection(toSection)){
            return 0;
        }
        return new HashSet<>(fromSection.getDebicodes()).equals(new HashSet<>(toSection.getDebicodes())) ? 0 : 1;
    }

    public static int isInSidingBetweenJournyes(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params){
        return isSidingHappening(fromSection,toSection,params) ? 1 : 0;
    }

    public static boolean hasChainAnEmptyTrip(List<RotaziunSection> sections, Pair<Integer, Integer> p) {
        return !Objects.equals(sections.get(p.getFirst()).getToStation().getId(), sections.get(p.getSecond()).getFromStation().getId());
    }

    public static boolean hasChainEmptyTrip(Pair<RotaziunSection, RotaziunSection> chain) {
        return !chain.getFirst().getToStation().getId().equals(chain.getSecond().getFromStation().getId());
    }

    static int computeValuedChainTime(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        return getIdleTimeWithoutMinTurnTime(fromSection, toSection, params) +
            (int) (EMPTY_TRIP_MULTIPLIER * getEmptyTripDuration(fromSection, toSection, params.getDurationMap())) +
            STAMM_CHANGE_ADDITION * isStammChange(fromSection, toSection, params);
    }

    private static boolean isChainInProhibitedSidingSite(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        return isSidingHappening(fromSection, toSection, params) &&
            params.getProhibitedSidingSites().contains(fromSection.getToStation().getId()) &&
            params.getProhibitedSidingSites().contains(toSection.getFromStation().getId());
    }

    private static boolean chainContainsProhibitedEmptyTrip(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        return params.getProhibitedEmptyTripsMap().containsKey(fromSection.getToStation().getId()) &&
            params.getProhibitedEmptyTripsMap().get(fromSection.getToStation().getId()).contains(toSection.getFromStation().getId());
    }

    public static boolean isChainEqual(Pair<RotaziunSection, RotaziunSection> chain1, Pair<RotaziunSection, RotaziunSection> chain2) {
        return Objects.equals(chain1.getFirst().getId(), chain2.getFirst().getId()) && Objects.equals(chain1.getSecond().getId(), chain2.getSecond().getId());
    }

}
