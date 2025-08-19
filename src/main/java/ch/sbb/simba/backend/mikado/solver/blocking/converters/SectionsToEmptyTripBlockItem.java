package ch.sbb.simba.backend.mikado.solver.blocking.converters;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getEmptyTripDuration;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.isSidingVariableNeeded;

import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlockItem;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunResultParams;

public final class SectionsToEmptyTripBlockItem {

    private SectionsToEmptyTripBlockItem() {
    }

    public static RotaziunBlockItem convert(RotaziunSection currentSection, RotaziunSection nextSection, IpSolverParams params, RotaziunResultParams resultParams) {

        int startTimeOfEmptyTrip = determineStartTimeOfEmptyTripInSectionChain(currentSection, nextSection, params, resultParams);
        startTimeOfEmptyTrip = startTimeOfEmptyTrip % DAY_IN_SECONDS;

        return RotaziunBlockItem.builder()
            .start(startTimeOfEmptyTrip)
            .end(startTimeOfEmptyTrip + getEmptyTripDuration(currentSection, nextSection, params.getDurationMap()))
            .fromStation(currentSection.getToStation())
            .toStation(nextSection.getFromStation())
            .type(RotaziunSectionType.EMPTY)
            .build();
    }

    // determine wether empty trip should be at the beginning or end of a chain
    // this depends on the siding capacity
    private static int determineStartTimeOfEmptyTripInSectionChain(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params, RotaziunResultParams resultParams) {

        if(params.getStageParams().isWithSidings() && isSidingVariableNeeded(params, fromSection, toSection) && resultParams.getSidingBeforeEmptyTrip().get(fromSection)){
            // empty trip must be before toSection
            return getStartTimeOfEmptyTripBeforeToSection(fromSection, toSection, params);
        }
        if(params.getProhibitedSidingSites().contains(toSection.getFromStation().getId())){
            return getStartTimeOfEmptyTripBeforeToSection(fromSection, toSection, params);
        }
        // by default, the empty trip is after the fromSection
        return (fromSection.getArrival() + params.getMinTurnTime()) % DAY_IN_SECONDS;

    }

    private static int getStartTimeOfEmptyTripBeforeToSection(RotaziunSection fromSection, RotaziunSection toSection, IpSolverParams params) {
        int startTime = (toSection.getDeparture() - getEmptyTripDuration(fromSection, toSection, params.getDurationMap()) - params.getMinTurnTime());
        startTime = startTime < 0 ? startTime + DAY_IN_SECONDS : startTime;
        return startTime;
    }

}
