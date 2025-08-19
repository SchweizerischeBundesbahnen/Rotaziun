package ch.sbb.simba.backend.mikado.solver.ip.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.parseChainsFromIdsToObjects;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunInputParams;
import ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation;
import ch.sbb.simba.backend.mikado.solver.utils.RotaziunDurationMap;
import ch.sbb.simba.backend.mikado.solver.utils.PreProcessCouplingDecoupling;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Getter
@Setter
public class IpSolverParams {

    // internal solver parameters
    private boolean twoStages;
    private int precisionLevel;
    private int numOfWorkers;

    private StageParams stageParams;

    private Map<RotaziunSection,Integer> sectionIdxMap;

    // basic constants
    public static final int MIN_NUMBER_OF_SECTIONS_FOR_TWO_STAGE_APPROACH = 30;
    public static final int EMPTY_TRIP_COST_PER_SECOND = 1;
    public static final int EMPTY_TRIP_COST_CONSTANT = 10;
    public static final int STAMM_CHANGE_PENALTY_FREE_PERIOD = 4*3600;

    private int minTurnTime;
    private int trainLength;
    private RotaziunDurationMap durationMap;

    // basic objectives
    private int vehicleCostPerDay;
    private int costPerStammChange;
    private int costPerSiding;
    private int costPerDebicodeChange;

    // chains and empty trips
    private Set<Pair<RotaziunSection,RotaziunSection>> requiredSectionChains;
    private Set<Pair<RotaziunSection,RotaziunSection>> prohibitedSectionChains;
    private Map<Long, Set<Long>> prohibitedEmptyTripsMap; // (from Station-ID, to prohibited Station-IDs)

    // prohibited siding sites
    private Set<Long> prohibitedSidingSites;

    // one block
    private boolean onlyOneBlock;

    // coupling & decoupling
    private boolean withCouplingDecoupling;
    private int costForCouplingDecoupling;
    private int minTimeForDecoupling;
    private int minTimeForCoupling;
    private Set<Long> prohibitedCouplingDecouplingStationIds;

    // coupling & decoupling internal parameters
    private Map<RotaziunSection,RotaziunSection> enforcementStammPairMap; // (verstaerker,stamm)
    private Map<RotaziunSection,Integer> enforcementSectionToEnforcementIdMap;
    private Map<Long,Integer> sectionToCouplingIdMap;
    private Map<Long,Integer> sectionToDecouplingIdMap;

    // Sidings
    private boolean withSidings;
    private int minSidingDuration;
    private int sidingEvaluationTime;
    private Map<Long, Integer> sidingCapacityMap; // (station-ID, available train siding length [meters])

    // Maintenance
    private boolean withMaintenance;
    private double maintenanceWindowDistributionTolerance; // between 0 and 1 -> 0: most equal distribution
    private List<RotaziunSection> maintenanceWindows;

    public IpSolverParams(RotaziunInputParams input, List<RotaziunSection> sections, List<RotaziunSection> maintenanceSections, RotaziunDurationMap durationMap) {

        this.durationMap = durationMap;

        // Get Input Parameters
        this.precisionLevel = input.getPrecisionLevel();
        this.numOfWorkers = input.getNumOfWorkers();
        this.minTurnTime = input.getMinTurnTime();
        this.vehicleCostPerDay = input.getVehicleCostPerDay();
        this.costPerStammChange = input.getCostPerStammChange();
        this.costPerSiding = input.getCostPerSiding();
        this.costPerDebicodeChange = input.getCostPerDebicodeChange();
        this.requiredSectionChains = parseChainsFromIdsToObjects(sections, input.getRequiredSectionChains());
        this.prohibitedSectionChains = parseChainsFromIdsToObjects(sections, input.getProhibitedSectionChains());
        this.prohibitedEmptyTripsMap = input.getProhibitedEmptyTripsMap();
        this.prohibitedSidingSites = input.getProhibitedSidingSites();
        this.onlyOneBlock = input.getOnlyOneBlock();
        this.costForCouplingDecoupling = input.getCostForCouplingDecoupling();
        this.minTimeForCoupling = input.getMinTimeForCoupling();
        this.minTimeForDecoupling = input.getMinTimeForDecoupling();
        this.prohibitedCouplingDecouplingStationIds = input.getProhibitedCouplingDecouplingStationIds();
        this.minSidingDuration = input.getMinSidingDuration();
        this.sidingEvaluationTime = input.getSidingEvaluationTime();
        this.sidingCapacityMap = input.getSidingCapacityMap();
        this.maintenanceWindowDistributionTolerance = input.getMaintenanceWindowDistributionTolerance();
        this.maintenanceWindows = maintenanceSections;

        // Set Internal Parameters
        this.withCouplingDecoupling = ModuleAcitvation.isCouplingDecouplingModuleNeeded(input);
        this.withSidings = ModuleAcitvation.isSidingModuleNeeded(input);
        this.withMaintenance = ModuleAcitvation.isMaintenaceModuleNeeded(maintenanceSections);
        this.twoStages = ModuleAcitvation.solveWithTwoStages(this, sections);
        this.trainLength = (int) sections.get(0).getVehicle().getLength();
        this.sectionIdxMap = computeSectionIdxMap(sections);

        // Set Coupling & Decouling Parameters
        if(this.isTwoStages() || this.isWithCouplingDecoupling()){
            this.enforcementStammPairMap = PreProcessCouplingDecoupling.findEnforcementStammPairs(sections);
        }
        if(this.isWithCouplingDecoupling()){
            this.enforcementSectionToEnforcementIdMap = PreProcessCouplingDecoupling.makeEnforcementToIdMap(this.enforcementStammPairMap);
            this.sectionToCouplingIdMap = PreProcessCouplingDecoupling.makeSectionToCouplingDecouplingMap(sections, true);
            this.sectionToDecouplingIdMap = PreProcessCouplingDecoupling.makeSectionToCouplingDecouplingMap(sections, false);
        }

        // add prohibited siding sites to siding capacity map if siding module is active
        if(this.isWithSidings()){
            this.prohibitedSidingSites.forEach(site -> this.sidingCapacityMap.put(site, 0));
        }

        ModuleAcitvation.validateModulUsage(this);
    }

    public IpSolverParams setStageOneParams(){
        this.stageParams = StageOne.getStageOneParams(this);
        return this;
    }

    public IpSolverParams setStageTwoParams(Map<RotaziunSection,RotaziunSection> sectionChainMap) {
        this.stageParams = StageTwo.getStageTwoParams(this, sectionChainMap);
        return this;
    }

    public IpSolverParams setMaintenanceStageParams(Map<RotaziunSection,RotaziunSection> sectionChainMap) {
        this.stageParams = MaintenanceStage.getMaintenanceStageParams(this, sectionChainMap);
        return this;
    }

    public Map<RotaziunSection,Integer> computeSectionIdxMap(List<RotaziunSection> sections) {
        return IntStream.range(0, sections.size()).boxed().collect(Collectors.toMap(sections::get, i -> i));
    }
}