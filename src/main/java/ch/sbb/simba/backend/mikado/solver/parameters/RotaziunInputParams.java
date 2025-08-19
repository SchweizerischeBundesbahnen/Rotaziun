package ch.sbb.simba.backend.mikado.solver.parameters;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RotaziunInputParams {

    // internal solver parameters
    private Integer precisionLevel; // determines the trade-off between computation speed and solution quality -> 0: fast solve, 1: high solution quality, 2: perfect solution quality
    private Integer numOfWorkers; // CP-SAT optimizer can use parallel computing; numOfWorkers defines how many solver instances are used

    // basic constants
    private Integer minTurnTime; // Minimum time required between two chained sections [s]
    private Integer minSidingDuration; // time after which a standstill is considered a siding [s]

    // basic objectives
    private Integer vehicleCostPerDay; // cost per blockday [s]
    private Integer costPerStammChange; // cost incurred when a vehicle changes service type from Stamm to Enforcement [s]
    private Integer costPerSiding; // cost for placing a vehicle in a siding between two chained sections [s]
    private Integer costPerDebicodeChange; // cost for changing debicode between two chained sections [s]

    // chains and empty trips
    private Set<Pair<Long,Long>> requiredSectionChains; // set of section pairs (specified by section ID) that must be chained directly together
    private Set<Pair<Long,Long>> prohibitedSectionChains; // set of section pairs (specified by section ID) that are not allowed to be chained together
    private Map<Long,Set<Long>> prohibitedEmptyTripsMap; // map specifying, for a given station (by station ID), which other stations are not allowed for empty trips

    // prohibited siding sites
    private Set<Long> prohibitedSidingSites; // set of stations (specified by station ID) where siding is not allowed

    // one block
    private Boolean onlyOneBlock; // enforces the use only one block

    // coupling & decoupling
    private Integer costForCouplingDecoupling; // cost for one coupling or decoupling of two vehicles [s]
    private Integer minTimeForDecoupling; // minimum time required for Enforcement, after being decoupled from Stamm, to continue with the next service [s]
    private Integer minTimeForCoupling; // minimum time required for Enforcement, after ending its previous service, to couple to its Stamm [s]
    private Set<Long> prohibitedCouplingDecouplingStationIds; // set of stations (specified by station ID) where coupling or decoupling is not allowed

    // Sidings
    private Integer sidingEvaluationTime; // evaluation time at which siding capacity restrictions are checked [s]
    private Map<Long,Integer> sidingCapacityMap; // map specifying the maximum available siding length (in meters) for each station (by station ID)

    // Maintenance
    private Double maintenanceWindowDistributionTolerance; // [0,1] -> 0: perfectly even distribution, 1: any placement allowed

}
