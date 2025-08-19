package ch.sbb.simba.backend.mikado.solver.parameters;

import java.util.HashMap;
import java.util.HashSet;

public final class InputDefaultValues {

    // internal solver parameters
    private static final int DEFAULT_PRECISION_LEVEL = 0;
    private static final int DEFAULT_NUM_OF_WORKERS = 8;

    // basic constants
    private static final int DEFAULT_MIN_TURN_TIME = 180;
    private static final int DEFAULT_MIN_SIDING_DURATION = 2*3600;

    // basic objectives
    private static final int DEFAULT_VEHICLE_COST_PER_DAY = 100*3600;
    private static final int DEFAULT_COST_PER_STAMM_CHANGE = 10*60;
    private static final int DEFAULT_COST_PER_SIDING = 60;
    private static final int DEFAULT_COST_PER_DEBICODE_CHANGE = 0;

    // one block
    private static final boolean DEFAULT_ONLY_ONE_BLOCK = true;

    // coupling & decoupling
    private static final int DEFAULT_COST_FOR_COUPLING_DECOUPLING = 0; // 4*60
    private static final int DEFAULT_MIN_TIME_FOR_DECOUPLING = 0;
    private static final int DEFAULT_MIN_TIME_FOR_COUPLING = 0;

    // sidings
    private static final int DEFAULT_SIDING_EVALUATION_TIME = 3*3600;

    // maintenance
    private static final double DEFAULT_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE = 0.3;


    private InputDefaultValues() {
    }

    public static void setDefaults(RotaziunInputParams input){

        setIntAndBoolDefaultValues(input);
        setMapAndSetDefaultValues(input);

    }

    private static void setIntAndBoolDefaultValues(RotaziunInputParams input) {
        input.setPrecisionLevel(defaultIfNull(input.getPrecisionLevel(), DEFAULT_PRECISION_LEVEL));
        input.setNumOfWorkers(defaultIfNull(input.getNumOfWorkers(), DEFAULT_NUM_OF_WORKERS));
        input.setMinTurnTime(defaultIfNull(input.getMinTurnTime(), DEFAULT_MIN_TURN_TIME));
        input.setMinSidingDuration(defaultIfNull(input.getMinSidingDuration(), DEFAULT_MIN_SIDING_DURATION));
        input.setVehicleCostPerDay(defaultIfNull(input.getVehicleCostPerDay(), DEFAULT_VEHICLE_COST_PER_DAY));
        input.setCostPerStammChange(defaultIfNull(input.getCostPerStammChange(), DEFAULT_COST_PER_STAMM_CHANGE));
        input.setCostPerSiding(defaultIfNull(input.getCostPerSiding(), DEFAULT_COST_PER_SIDING));
        input.setCostPerDebicodeChange(defaultIfNull(input.getCostPerDebicodeChange(), DEFAULT_COST_PER_DEBICODE_CHANGE));
        input.setOnlyOneBlock(defaultIfNull(input.getOnlyOneBlock(), DEFAULT_ONLY_ONE_BLOCK));
        input.setCostForCouplingDecoupling(defaultIfNull(input.getCostForCouplingDecoupling(), DEFAULT_COST_FOR_COUPLING_DECOUPLING));
        input.setMinTimeForDecoupling(defaultIfNull(input.getMinTimeForDecoupling(), DEFAULT_MIN_TIME_FOR_DECOUPLING));
        input.setMinTimeForCoupling(defaultIfNull(input.getMinTimeForCoupling(), DEFAULT_MIN_TIME_FOR_COUPLING));
        input.setSidingEvaluationTime(defaultIfNull(input.getSidingEvaluationTime(), DEFAULT_SIDING_EVALUATION_TIME));
        input.setMaintenanceWindowDistributionTolerance(defaultIfNull(input.getMaintenanceWindowDistributionTolerance(), DEFAULT_MAINTENANCE_WINDOW_DISTRIBUTION_TOLERANCE));
    }

    private static void setMapAndSetDefaultValues(RotaziunInputParams input) {
        input.setRequiredSectionChains(defaultIfNull(input.getRequiredSectionChains(), new HashSet<>()));
        input.setProhibitedSectionChains(defaultIfNull(input.getProhibitedSectionChains(), new HashSet<>()));
        input.setProhibitedEmptyTripsMap(defaultIfNull(input.getProhibitedEmptyTripsMap(), new HashMap<>()));
        input.setProhibitedSidingSites(defaultIfNull(input.getProhibitedSidingSites(), new HashSet<>()));
        input.setProhibitedCouplingDecouplingStationIds(defaultIfNull(input.getProhibitedCouplingDecouplingStationIds(), new HashSet<>()));
        input.setSidingCapacityMap(defaultIfNull(input.getSidingCapacityMap(), new HashMap<>()));
    }

    private static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

}