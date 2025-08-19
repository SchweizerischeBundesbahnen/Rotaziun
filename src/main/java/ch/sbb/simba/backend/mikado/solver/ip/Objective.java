package ch.sbb.simba.backend.mikado.solver.ip;

import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getCost;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getEmptyTripDuration;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isDifferentDebicodes;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isInSidingBetweenJournyes;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isStammChange;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class Objective {

    private Objective() {
    }

    private static int minTurnTime;

    static void makeObjective(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, List<RotaziunSection> sections, IpSolverParams params) {

        minTurnTime = params.getMinTurnTime();
        MPObjective objective = solver.objective();
        objective.setMinimization();

        for(Pair<Integer,Integer> chain : possibleChains){
            RotaziunSection fromSection = sections.get(chain.getFirst());
            RotaziunSection toSection = sections.get(chain.getSecond());
            int emptyTripDuration = getEmptyTripDuration(fromSection, toSection, params.getDurationMap());
            objective.setCoefficient(v.xMap.get(getKey(chain)), getCost(emptyTripDuration) +
                    params.getCostPerStammChange() * isStammChange(fromSection, toSection, params) +
                    params.getVehicleCostPerDay() * getNumberOfBlockDays(fromSection, toSection, emptyTripDuration) +
                    params.getCostPerDebicodeChange() * isDifferentDebicodes(fromSection, toSection, params) +
                    params.getCostPerSiding() * isInSidingBetweenJournyes(fromSection, toSection, params));
        }

        if(params.getStageParams().isWithCouplingDecoupling()){
            for(int idx = 0; idx < v.decoup.length; idx++){
                objective.setCoefficient(v.decoup[idx], params.getCostForCouplingDecoupling());
            }
        }

    }

    public static int getNumberOfBlockDays(RotaziunSection fromSection, RotaziunSection toSection, int emptyTripDuration) {

        Pair<Integer,Integer> fromSectionTimes = new Pair<>(fromSection.getDeparture(), fromSection.getArrival());
        Pair<Integer,Integer> toSectionTimes = new Pair<>(toSection.getDeparture(), toSection.getArrival());

        if(emptyTripDuration == 0){
            return startsNewBlockDay(fromSectionTimes, toSectionTimes);
        }

        Pair<Integer,Integer> emptyTripTimes = new Pair<>((fromSection.getArrival()+minTurnTime) % DAY_IN_SECONDS, (fromSection.getArrival()+minTurnTime+emptyTripDuration) % DAY_IN_SECONDS);

        // some section chain require two new block days
        return startsNewBlockDay(fromSectionTimes, emptyTripTimes) + startsNewBlockDay(emptyTripTimes, toSectionTimes);
    }

    private static int startsNewBlockDay(Pair<Integer,Integer> fromSection, Pair<Integer,Integer> toSection) {
        assert(fromSection.getSecond() <= DAY_IN_SECONDS);
        return (fromSection.getSecond() < fromSection.getFirst() || toSection.getFirst() - fromSection.getSecond() - minTurnTime < 0) ? 1 : 0;
    }

}