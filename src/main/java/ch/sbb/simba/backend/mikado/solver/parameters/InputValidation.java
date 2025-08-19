package ch.sbb.simba.backend.mikado.solver.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.parseChainsFromIdsToObjects;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.hasChainEmptyTrip;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isChainEqual;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isSidingHappening;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;
import ch.sbb.simba.backend.mikado.solver.utils.RotaziunDurationMap;
import java.util.List;
import java.util.Set;

public final class InputValidation {

    private InputValidation(){
    }

    // internal solver parameters
    private static final int MIN_PRECISION_LEVEL = 0;
    public static final int MAX_PRECISION_LEVEL = 2;
    private static final int MIN_NUM_OF_WORKERS = 1;
    private static final int MAX_NUM_OF_WORKERS = 16;

    // basic constants
    private static final int MIN_TURN_TIME_LIMIT = 12*3600;
    private static final int MIN_SIDING_DURATION_LOWER_LIMIT = 60;
    private static final int MIN_SIDING_DURATION_UPPER_LIMIT = 22*3600;

    // basic objectives
    private static final int MAX_VEHICLE_COST_PER_DAY = 1000*3600;
    private static final int MAX_COST_PER_STAMM_CHANGE = 1000*3600;
    private static final int MAX_COST_PER_SIDING = 1000*3600;
    private static final int MAX_COST_PER_DEBICODE_CHANGE = 1000*3600;

    // coupling & decoupling
    private static final int MAX_COST_FOR_COUPLING_DECOUPLING = 1000*3600;
    private static final int MIN_TIME_FOR_DECOUPLING_LIMIT = 12*3600;
    private static final int MIN_TIME_FOR_COUPLING_LIMIT = 12*3600;

    // sidings
    private static final int MIN_SIDING_EVALUATION_TIME = 10;
    private static final int MAX_SIDING_EVALUATION_TIME = DAY_IN_SECONDS-10;

    // maintenance
    private static final double MIN_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE = 0.0;
    private static final double MAX_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE = 1.0;


    public static void validateInputParameters(List<RotaziunSection> sections, RotaziunInputParams input, RotaziunDurationMap durationMap) {

        validateInputParameterRange(input);

        Set<Pair<RotaziunSection, RotaziunSection>> requiredChains = parseChainsFromIdsToObjects(sections, input.getRequiredSectionChains());
        Set<Pair<RotaziunSection, RotaziunSection>> prohibitedChains = parseChainsFromIdsToObjects(sections, input.getProhibitedSectionChains());

        testRequiredChainsNotConflicProhibitedChains(requiredChains,prohibitedChains);
        testRequiredChainsNotContainProhibitedEmptyTrips(requiredChains,input);
        testRequiredChainsAreNotInProhibitedSidingSites(requiredChains, input, durationMap);

        testProhibitedSidingSitesNotInSidingCapacityMap(input);

    }

    private static void validateInputParameterRange(RotaziunInputParams input) {
        checkRange("precisionLevel", input.getPrecisionLevel(), MIN_PRECISION_LEVEL, MAX_PRECISION_LEVEL);
        checkRange("numOfWorkers", input.getNumOfWorkers(), MIN_NUM_OF_WORKERS, MAX_NUM_OF_WORKERS);
        checkRange("minTurnTime", input.getMinTurnTime(), 0, MIN_TURN_TIME_LIMIT);
        checkRange("minSidingDuration", input.getMinSidingDuration(), MIN_SIDING_DURATION_LOWER_LIMIT, MIN_SIDING_DURATION_UPPER_LIMIT);
        checkRange("vehicleCostPerDay", input.getVehicleCostPerDay(), 0, MAX_VEHICLE_COST_PER_DAY);
        checkRange("costPerStammChange", input.getCostPerStammChange(), 0, MAX_COST_PER_STAMM_CHANGE);
        checkRange("costPerSiding", input.getCostPerSiding(), 0, MAX_COST_PER_SIDING);
        checkRange("costPerDebicodeChange", input.getCostPerDebicodeChange(), 0, MAX_COST_PER_DEBICODE_CHANGE);
        checkRange("costForCouplingDecoupling", input.getCostForCouplingDecoupling(), 0, MAX_COST_FOR_COUPLING_DECOUPLING);
        checkRange("minTimeForDecoupling", input.getMinTimeForDecoupling(), 0, MIN_TIME_FOR_DECOUPLING_LIMIT);
        checkRange("minTimeForCoupling", input.getMinTimeForCoupling(), 0, MIN_TIME_FOR_COUPLING_LIMIT);
        checkRange("sidingEvaluationTime", input.getSidingEvaluationTime(), MIN_SIDING_EVALUATION_TIME, MAX_SIDING_EVALUATION_TIME);
        checkRange("maintenanceWindowDistributionTolerance", input.getMaintenanceWindowDistributionTolerance(),
            MIN_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE, MAX_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE);
    }

    private static void testRequiredChainsNotConflicProhibitedChains(Set<Pair<RotaziunSection, RotaziunSection>> requiredChainsDto, Set<Pair<RotaziunSection, RotaziunSection>> prohibitedChains) {
        for (Pair<RotaziunSection, RotaziunSection> requiredChain : requiredChainsDto) {
            for (Pair<RotaziunSection, RotaziunSection> prohibitedChain : prohibitedChains) {
                if (isChainEqual(requiredChain, prohibitedChain)) {
                    throw new IllegalArgumentException("Required section chain: " + getChainNameString(requiredChain) + " is also contained in the prohibited chain list");
                }
            }
        }
    }

    private static void testRequiredChainsNotContainProhibitedEmptyTrips(Set<Pair<RotaziunSection, RotaziunSection>> requiredChainsDto, RotaziunInputParams input) {
        for (Pair<RotaziunSection, RotaziunSection> requiredChain : requiredChainsDto) {
            if(hasChainEmptyTrip(requiredChain)){
                Set<Long> prohibitedToStationIds = input.getProhibitedEmptyTripsMap().get(requiredChain.getFirst().getToStation().getId());
                if (prohibitedToStationIds != null && prohibitedToStationIds.contains(requiredChain.getSecond().getFromStation().getId())) {
                    throw new IllegalArgumentException("Required section chain: \n" + getChainNameString(requiredChain) +
                        "contains prohibited empty trip from: " + requiredChain.getFirst().getToStation().getName() + " to: " + requiredChain.getSecond().getFromStation().getName());
                }
            }
        }
    }

    private static void testRequiredChainsAreNotInProhibitedSidingSites(Set<Pair<RotaziunSection, RotaziunSection>> requiredChainsDto, RotaziunInputParams input, RotaziunDurationMap durationMap) {
        for (Pair<RotaziunSection, RotaziunSection> requiredChain : requiredChainsDto) {
            if(isSidingHappening(requiredChain, input, durationMap)){
                if(input.getProhibitedSidingSites().contains(requiredChain.getFirst().getToStation().getId())
                    && input.getProhibitedSidingSites().contains(requiredChain.getSecond().getFromStation().getId())){
                    throw new IllegalArgumentException("Required section chain: \n" + getChainNameString(requiredChain) +
                        "has a siding in prohibited siding site: " + requiredChain.getFirst().getToStation().getName() + " and: " + requiredChain.getSecond().getFromStation().getName());
                }
            }
        }
    }

    private static void testProhibitedSidingSitesNotInSidingCapacityMap(RotaziunInputParams input) {
        for (Long prohibitedSidingSiteId : input.getProhibitedSidingSites()) {
            if (input.getSidingCapacityMap().containsKey(prohibitedSidingSiteId)) {
                throw new IllegalArgumentException("Prohibited Siding Site (ID: " + prohibitedSidingSiteId + ") should not be contained in sidingCapacityMap");
            }
        }
    }

    private static String getChainNameString(Pair<RotaziunSection, RotaziunSection> chain) {
        return "fromSection: " + chain.getFirst().getFromStation().getName() + " -> " + chain.getFirst().getToStation().getName() + "\n" +
            "toSection: " + chain.getSecond().getFromStation().getName() + " -> " + chain.getSecond().getToStation().getName() + "\n";
    }

    private static void checkRange(String paramName, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(paramName + " must be between " + min + " and " + max);
        }
    }

    private static void checkRange(String paramName, double value, double min, double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(paramName + " must be between " + min + " and " + max);
        }
    }

}