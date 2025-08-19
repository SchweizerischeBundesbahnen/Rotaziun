package ch.sbb.simba.backend.mikado.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunStation;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunVehicle;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunInputParams;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RotaziunSolverTest {

    RotaziunStation station1;
    RotaziunStation station2;
    RotaziunStation station3;
    RotaziunVehicle vehicle1;

    @BeforeEach
    void init() {
        this.station1 = RotaziunStation.builder().id(1001L).name("Station 1").xcoord(0f).ycoord(0f).build();
        this.station2 = RotaziunStation.builder().id(1002L).name("Station 2").xcoord(0f).ycoord(1000f).build();
        this.station3 = RotaziunStation.builder().id(1003L).name("Station 3").xcoord(1000f).ycoord(0f).build();
        this.vehicle1 = RotaziunVehicle.builder().name("Vehicle1").length(1.0F).id(1L).build();

    }

    @Test
    void testSolveOneSection() {
        RotaziunInputParams params = setBasicIpSolverParams(180);
        var sections = List.of(getStammSection(station1, station2, 6 * 3600, 8 * 3600, 1L, 1L));
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();

        assertEquals(1, blocks.size());
        assertEquals(1, blocks.get(0).getDays().size());
        assertEquals(2, blocks.get(0).getDays().get(0).getItems().size());
        assertEquals(station1, blocks.get(0).getDays().get(0).getItems().get(0).getFromStation());
        assertEquals(station2, blocks.get(0).getDays().get(0).getItems().get(0).getToStation());
        assertEquals(6 * 3600, blocks.get(0).getDays().get(0).getItems().get(0).getStart());
        assertEquals(8 * 3600, blocks.get(0).getDays().get(0).getItems().get(0).getEnd());
        assertEquals(station2, blocks.get(0).getDays().get(0).getItems().get(1).getFromStation());
        assertEquals(station1, blocks.get(0).getDays().get(0).getItems().get(1).getToStation());
        assertEquals(8 * 3600 + params.getMinTurnTime(), blocks.get(0).getDays().get(0).getItems().get(1).getStart());
        assertEquals(10 * 3600 + params.getMinTurnTime(), blocks.get(0).getDays().get(0).getItems().get(1).getEnd());
    }

    @Test
    void testSolveTwoSection() {
        RotaziunInputParams params = setBasicIpSolverParams(180);
        var sections = List.of(getStammSection(station1, station2, 6 * 3600, 8 * 3600, 1L, 1L), getStammSection(station1, station2, 6 * 3600, 8 * 3600, 2L, 2L));
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();

        assertEquals(1, blocks.size());
        assertEquals(2, blocks.get(0).getDays().size());
        assertEquals(2, blocks.get(0).getDays().get(0).getItems().size());
    }

    @Test
    void testUserDefinedRequiredChainResultsInTwoBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 6 * 3600, 10 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station2, 11 * 3600, 18 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 19 * 3600, 23 * 3600, 3L, 3L);
        var sections = List.of(section1, section2, section3);

        params.setRequiredSectionChains(new HashSet<>(Set.of(new Pair<>(1L, 3L))));
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params.setRequiredSectionChains(new HashSet<>(Set.of()));
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(1, blocks.get(0).getDays().size());

    }

    @Test
    void testUserDefinedProhibitedChainsResultsInTwoBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 6 * 3600, 10 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station2, 11 * 3600, 18 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 19 * 3600, 23 * 3600, 3L, 3L);
        var sections = List.of(section1, section2, section3);

        params.setProhibitedSectionChains(new HashSet<>(Set.of(new Pair<>(1L, 2L), new Pair<>(2L, 3L))));
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params.setProhibitedSectionChains(new HashSet<>(Set.of()));
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(1, blocks.get(0).getDays().size());

    }

    @Test
    void testUserDefinedProhibitedEmptyTripResultsInTwoBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station3, 4 * 3600, 5 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station3, station1, 6 * 3600, 7 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 8 * 3600, 10 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station3, 11 * 3600, 12 * 3600, 4L, 4L);
        RotaziunSection section5 = getStammSection(station2, station1, 14 * 3600, 23 * 3600, 5L, 5L);

        var sections = List.of(section1, section2, section3, section4, section5);

        Map<Long, Set<Long>> prohibitedEmptyTrips = new HashMap<>();
        prohibitedEmptyTrips.put(1003L, Set.of(1002L));
        params.setProhibitedEmptyTripsMap(prohibitedEmptyTrips);
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params.setProhibitedEmptyTripsMap(new HashMap<>());
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(1, blocks.get(0).getDays().size());

    }

    @Test
    void testProhibitedSidingSitesResultsInTwoBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station3, 18 * 3600, 19 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station3, station1, 20 * 3600, 21 * 3600, 4L, 4L);

        var sections = List.of(section1, section2, section3, section4);

        params.setProhibitedSidingSites(Set.of(1002L));
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params.setProhibitedSidingSites(new HashSet<>());
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(1, blocks.get(0).getDays().size());

    }

    @Test
    void testProhibitedSidingSitesResultsInThreeBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station3, 11 * 3600, 12 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 4L, 4L);
        RotaziunSection section5 = getStammSection(station1, station3, 11 * 3600, 12 * 3600, 5L, 5L);
        var sections = List.of(section1, section2, section3, section4, section5);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        Set<Long> prohibitedSidingSites = new HashSet<>();
        prohibitedSidingSites.add(1001L);
        params.setProhibitedSidingSites(prohibitedSidingSites);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(3, blocks.get(0).getDays().size());
        assertEquals(4, blocks.get(0).getDays().stream().map(d -> d.getItems().size()).max(Integer::compareTo).orElse(0));
        assertEquals(1, blocks.get(0).getDays().stream().map(d -> d.getItems().size()).min(Integer::compareTo).orElse(0));

    }

    @Test
    void testOneBlockResultsInTwoBlocks() {
        RotaziunInputParams params = setBasicIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 6 * 3600, 10 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 11 * 3600, 18 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 5 * 3600, 11 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station1, station2, 12 * 3600, 23 * 3600, 4L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.size());
        assertEquals(2, blocks.get(0).getDays().size() + blocks.get(1).getDays().size());

        params = setOneBlockIpSolverParams(180);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(1, blocks.size());
        assertEquals(2, blocks.get(0).getDays().size());

    }

    @Test
    void testLimitedSidingCapacityResultsInThreeBlockDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        Map<Long, Integer> sidingCapacityMap = new HashMap<>();
        sidingCapacityMap.put(1001L, 1);
        params.setSidingCapacityMap(sidingCapacityMap);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(3, blocks.get(0).getDays().size());
        assertEquals(4, blocks.get(0).getDays().stream().map(d -> d.getItems().size()).max(Integer::compareTo).orElse(0));
        assertEquals(1, blocks.get(0).getDays().stream().map(d -> d.getItems().size()).min(Integer::compareTo).orElse(0));

    }

    @Test
    void testSidingEvaluationTimeNotAffectsSolution() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        Map<Long, Integer> sidingCapacityMap = new HashMap<>();
        sidingCapacityMap.put(1001L, 1);
        params.setSidingCapacityMap(sidingCapacityMap);
        params.setSidingEvaluationTime(12 * 3600);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

    }

    @Test
    void testLimitedSidingCapacityResultsInNoSolution() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        Map<Long, Integer> sidingCapacityMap = new HashMap<>();
        sidingCapacityMap.put(1001L, 1);
        sidingCapacityMap.put(1002L, 1);
        params.setSidingCapacityMap(sidingCapacityMap);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(0, blocks.size());

    }

    @Test
    void testMinimizeDecouplingResultsInMoreDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        RotaziunSection section5 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 5L);
        RotaziunSection section6 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 6L);
        RotaziunSection section7 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 7L);
        RotaziunSection section8 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 8L);
        RotaziunSection section9 = getStammSection(station1, station2, 12 * 3600, 13 * 3600, 5L, 9L);
        RotaziunSection section10 = getStammSection(station1, station2, 20 * 3600, 21 * 3600, 6L, 10L);
        var sections = List.of(section1, section2, section3, section4, section5, section6, section7, section8, section9, section10);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(4, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setCostForCouplingDecoupling(200 * 3600);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(5, blocks.get(0).getDays().size());

    }

    @Test
    void testProhibitedCouplingDecouplingSiteResultsInMoreDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        RotaziunSection section5 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 5L);
        RotaziunSection section6 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 6L);
        RotaziunSection section7 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 7L);
        RotaziunSection section8 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 8L);
        RotaziunSection section9 = getStammSection(station1, station2, 12 * 3600, 13 * 3600, 5L, 9L);
        RotaziunSection section10 = getStammSection(station1, station2, 20 * 3600, 21 * 3600, 6L, 10L);
        var sections = List.of(section1, section2, section3, section4, section5, section6, section7, section8, section9, section10);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(4, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setProhibitedCouplingDecouplingStationIds(Set.of(1002L));
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(5, blocks.get(0).getDays().size());

    }

    @Test
    void testProhibitedCouplingDecouplingSitesResultsInNoSolution() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 2L);
        RotaziunSection section3 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 4L);
        RotaziunSection section5 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 5L);
        RotaziunSection section6 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 2L, 6L);
        RotaziunSection section7 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 3L, 7L);
        RotaziunSection section8 = getEnforcementSection(station2, station1, 16 * 3600, 17 * 3600, 4L, 8L);
        RotaziunSection section9 = getStammSection(station1, station2, 12 * 3600, 13 * 3600, 5L, 9L);
        RotaziunSection section10 = getStammSection(station1, station2, 20 * 3600, 21 * 3600, 6L, 10L);
        var sections = List.of(section1, section2, section3, section4, section5, section6, section7, section8, section9, section10);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(4, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setProhibitedCouplingDecouplingStationIds(Set.of(1001L, 1002L));
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertTrue(blocks.isEmpty());

    }

    @Test
    void testHighDecouplingTimeResultsInMoreDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 9 * 3600, 10 * 3600, 2L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 9 * 3600, 10 * 3600, 3L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setMinTimeForDecoupling(3601);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(3, blocks.get(0).getDays().size());

    }

    // decoupling time exactly matches limit
    @Test
    void testHighDecouplingTimeResultsInSameDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 9 * 3600, 10 * 3600, 2L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 9 * 3600, 10 * 3600, 3L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setMinTimeForDecoupling(3600);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

    }

    @Test
    void testHighCouplingTimeResultsInSameDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 1L);
        RotaziunSection section2 = getEnforcementSection(station1, station2, 7 * 3600, 8 * 3600, 1L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 9 * 3600, 10 * 3600, 2L, 3L);
        RotaziunSection section4 = getStammSection(station2, station1, 9 * 3600, 11 * 3600, 3L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setMinTimeForCoupling(3600);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

    }

    @Test
    void testProhibitedDecouplingSiteForPartialEnhancementResultsInSameDays() {
        RotaziunInputParams params = setDecouplingIpSolverParams(180);
        params.setProhibitedCouplingDecouplingStationIds(Set.of(1002L));

        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 9 * 3600, 1L, 1L);
        RotaziunSection section2 = getEnforcementSection(station1, station3, 7 * 3600, 8 * 3600, 1L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 10 * 3600, 11 * 3600, 2L, 3L);
        RotaziunSection section4 = getStammSection(station3, station1, 9 * 3600, 11 * 3600, 3L, 4L);
        var sections = List.of(section1, section2, section3, section4);
        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

    }

    @Test
    public void testProhibitedDecouplingSiteForPartialEnhancementResultsInNoSolution() {
        assertThrows(IllegalArgumentException.class, () -> {
            RotaziunInputParams params = setDecouplingIpSolverParams(180);
            params.setProhibitedCouplingDecouplingStationIds(Set.of(1003L));

            RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 9 * 3600, 1L, 1L);
            RotaziunSection section2 = getEnforcementSection(station1, station3, 7 * 3600, 8 * 3600, 1L, 2L);
            RotaziunSection section3 = getStammSection(station2, station1, 10 * 3600, 11 * 3600, 2L, 3L);
            RotaziunSection section4 = getStammSection(station3, station1, 9 * 3600, 11 * 3600, 3L, 4L);
            var sections = List.of(section1, section2, section3, section4);

            RotaziunSolver.solve(sections, List.of(), params);
        });
    }

    @Test
    void testLongDecouplingTimeForPartialEnhancementResultsInMoreDays() {
        RotaziunInputParams params = setOneBlockIpSolverParams(180);
        RotaziunSection section1 = getStammSection(station1, station2, 7 * 3600, 9 * 3600, 1L, 1L);
        RotaziunSection section2 = getEnforcementSection(station1, station3, 7 * 3600, 8 * 3600, 1L, 2L);
        RotaziunSection section3 = getStammSection(station2, station1, 10 * 3600, 11 * 3600, 2L, 3L);
        RotaziunSection section4 = getStammSection(station3, station1, 9 * 3600, 11 * 3600, 3L, 4L);
        var sections = List.of(section1, section2, section3, section4);

        var resultParams = RotaziunSolver.solve(sections, List.of(), params);
        var blocks = resultParams.getBlocks();
        assertEquals(2, blocks.get(0).getDays().size());

        params = setDecouplingIpSolverParams(180);
        params.setMinTimeForDecoupling(2 * 3600);
        resultParams = RotaziunSolver.solve(sections, List.of(), params);
        blocks = resultParams.getBlocks();
        assertEquals(3, blocks.get(0).getDays().size());

    }

    private RotaziunSection getStammSection(RotaziunStation fromStation, RotaziunStation toStation, int startTime, int endTime, long journeyID, long id) {
        return RotaziunSection.builder()
            .departure(startTime)
            .arrival(endTime)
            .isStamm(true)
            .debicodes(List.of(1))
            .fromStation(fromStation)
            .toStation(toStation)
            .vehicle(vehicle1)
            .journeyId(journeyID)
            .id(id)
            .sectionType(RotaziunSectionType.COMMERCIAL)
            .build();
    }

    private RotaziunSection getEnforcementSection(RotaziunStation fromStation, RotaziunStation toStation, int startTime, int endTime, long journeyID, long id) {
        return RotaziunSection.builder()
            .departure(startTime)
            .arrival(endTime)
            .isStamm(false)
            .journeyId(journeyID)
            .id(id)
            .debicodes(List.of(1))
            .fromStation(fromStation)
            .toStation(toStation)
            .vehicle(vehicle1)
            .sectionType(RotaziunSectionType.COMMERCIAL)
            .build();
    }

    private RotaziunInputParams setDecouplingIpSolverParams(int minTurnTime) {

        return RotaziunInputParams.builder()
            .precisionLevel(0)
            .numOfWorkers(8)
            .minTurnTime(minTurnTime)
            .vehicleCostPerDay(100 * 60 * 60)
            .costPerStammChange(10 * 60)
            .costPerSiding(10 * 60)
            .costPerDebicodeChange(0)
            .requiredSectionChains(new HashSet<>())
            .prohibitedSectionChains(new HashSet<>())
            .prohibitedEmptyTripsMap(new HashMap<>())
            .prohibitedSidingSites(new HashSet<>())
            .onlyOneBlock(true)
            .costForCouplingDecoupling(10 * 60)
            .minTimeForDecoupling(0)
            .minTimeForCoupling(0)
            .prohibitedCouplingDecouplingStationIds(new HashSet<>())
            .minSidingDuration(2 * 3600)
            .sidingEvaluationTime(3 * 3600)
            .sidingCapacityMap(new HashMap<>())
            .maintenanceWindowDistributionTolerance(0.3)
            .build();
    }

    private RotaziunInputParams setOneBlockIpSolverParams(int minTurnTime) {

        return RotaziunInputParams.builder()
            .precisionLevel(0)
            .numOfWorkers(8)
            .minTurnTime(minTurnTime)
            .vehicleCostPerDay(100 * 60 * 60)
            .costPerStammChange(10 * 60)
            .costPerSiding(10 * 60)
            .costPerDebicodeChange(0)
            .requiredSectionChains(new HashSet<>())
            .prohibitedSectionChains(new HashSet<>())
            .prohibitedEmptyTripsMap(new HashMap<>())
            .prohibitedSidingSites(new HashSet<>())
            .onlyOneBlock(true)
            .costForCouplingDecoupling(0)
            .minTimeForDecoupling(0)
            .minTimeForCoupling(0)
            .prohibitedCouplingDecouplingStationIds(new HashSet<>())
            .minSidingDuration(2 * 3600)
            .sidingEvaluationTime(3 * 3600)
            .sidingCapacityMap(new HashMap<>())
            .maintenanceWindowDistributionTolerance(0.3)
            .build();
    }

    private RotaziunInputParams setBasicIpSolverParams(int minTurnTime) {

        return RotaziunInputParams.builder()
            .precisionLevel(0)
            .numOfWorkers(8)
            .minTurnTime(minTurnTime)
            .vehicleCostPerDay(100 * 60 * 60)
            .costPerStammChange(10 * 60)
            .costPerSiding(10 * 60)
            .costPerDebicodeChange(0)
            .requiredSectionChains(new HashSet<>())
            .prohibitedSectionChains(new HashSet<>())
            .prohibitedEmptyTripsMap(new HashMap<>())
            .prohibitedSidingSites(new HashSet<>())
            .onlyOneBlock(false)
            .costForCouplingDecoupling(0)
            .minTimeForDecoupling(0)
            .minTimeForCoupling(0)
            .prohibitedCouplingDecouplingStationIds(new HashSet<>())
            .minSidingDuration(2 * 3600)
            .sidingEvaluationTime(3 * 3600)
            .sidingCapacityMap(new HashMap<>())
            .maintenanceWindowDistributionTolerance(0.3)
            .build();
    }

}
