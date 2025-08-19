package ch.sbb.simba.backend.mikado.solver.ip.constraints;

import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.INFINITY;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.mapToPositiveTimeDifference;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getEmptyTripDuration;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getIdleTime;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.hasChainAnEmptyTrip;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.isSidingVariableNeeded;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import java.util.Objects;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ConstrSidings {

    public static final int M_DETECT_SIDINGS = 2;


    private ConstrSidings() {
    }

    public static void makeSidingConstraints(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, List<RotaziunSection> sections, IpSolverParams params) {

        restrictSidingVariables(solver, possibleChains, v, sections, params);
        detectSiding(solver, possibleChains, v, sections, params);
        limitSidingSitesCapacityAtSidingEvaluationTime(solver, possibleChains, v, sections, params);

    }

    // set sidingVariables s_sart/ s_end to 0 if chain is not active
    private static void restrictSidingVariables(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, List<RotaziunSection> sections, IpSolverParams params) {

        MPConstraint[] constraintSidingResActiveChainA = new MPConstraint[possibleChains.size()];
        MPConstraint[] constraintSidingResActiveChainB = new MPConstraint[possibleChains.size()];

        int idx = 0;
        for (Pair<Integer, Integer> p : possibleChains) {
            if(isSidingVariableNeeded(params, sections, p)) {

                constraintSidingResActiveChainA[idx] = solver.makeConstraint(-INFINITY, 0, "sidingZeroA_" + idx);
                constraintSidingResActiveChainA[idx].setCoefficient(v.sMap.get(getKey(p)).getFirst(), 1);
                constraintSidingResActiveChainA[idx].setCoefficient(v.xMap.get(getKey(p)), -1);

                if(hasChainAnEmptyTrip(sections, p)) {
                    constraintSidingResActiveChainB[idx] = solver.makeConstraint(-INFINITY, 0, "sidingZeroB_" + idx);
                    constraintSidingResActiveChainB[idx].setCoefficient(v.sMap.get(getKey(p)).getSecond(), 1);
                    constraintSidingResActiveChainB[idx].setCoefficient(v.xMap.get(getKey(p)), -1);
                }

                idx++;
            }
        }
    }

    // constraintSLa
    // SUM_d {sl_i,j,d} + m*x_i,j <= isSL_i,j + m

    // constraintSLb
    // -SUM_d {sl_i,j,d} + m*x_i,j <= -isSL_i,j + m

    private static void detectSiding(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, List<RotaziunSection> sections, IpSolverParams params) {

        MPConstraint[] constraintSLa = new MPConstraint[possibleChains.size()];
        MPConstraint[] constraintSLb = new MPConstraint[possibleChains.size()];

        int idx = 0;
        for (Pair<Integer, Integer> p : possibleChains) {
            if(isSidingVariableNeeded(params, sections, p)) {

                constraintSLa[idx] = solver.makeConstraint(-INFINITY, M_DETECT_SIDINGS + 1, "detectSidingA_" + idx);
                constraintSLa[idx].setCoefficient(v.xMap.get(getKey(p)), M_DETECT_SIDINGS);
                constraintSLa[idx].setCoefficient(v.sMap.get(getKey(p)).getFirst(), 1);
                if(hasChainAnEmptyTrip(sections, p)){
                    constraintSLa[idx].setCoefficient(v.sMap.get(getKey(p)).getSecond(), 1);
                }

                constraintSLb[idx] = solver.makeConstraint(-INFINITY, M_DETECT_SIDINGS - 1, "detectSidingB_" + idx);
                constraintSLb[idx].setCoefficient(v.xMap.get(getKey(p)), M_DETECT_SIDINGS);
                constraintSLb[idx].setCoefficient(v.sMap.get(getKey(p)).getFirst(), -1);
                if(hasChainAnEmptyTrip(sections, p)) {
                    constraintSLb[idx].setCoefficient(v.sMap.get(getKey(p)).getSecond(), -1);
                }

                idx++;
            }
        }

    }

    private static void limitSidingSitesCapacityAtSidingEvaluationTime(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, List<RotaziunSection> sections, IpSolverParams params) {

        MPConstraint[] constraintSidingCapacity = new MPConstraint[params.getSidingCapacityMap().size()];
        int idx = 0;
        for (Long stationID : params.getSidingCapacityMap().keySet()) {

            constraintSidingCapacity[idx] = solver.makeConstraint(-INFINITY, params.getSidingCapacityMap().get(stationID), "sidingCap_" + idx);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(isSidingVariableNeeded(params, sections, p)) {
                    if (Objects.equals(sections.get(p.getFirst()).getToStation().getId(), stationID) && isInSidingEvaluationTime(sections, p, params, false)) {
                        constraintSidingCapacity[idx].setCoefficient(v.sMap.get(getKey(p)).getFirst(), params.getTrainLength());
                    }
                    if (hasChainAnEmptyTrip(sections, p) && Objects.equals(sections.get(p.getSecond()).getFromStation().getId(), stationID) && isInSidingEvaluationTime(sections, p, params, true)) {
                        constraintSidingCapacity[idx].setCoefficient(v.sMap.get(getKey(p)).getSecond(), params.getTrainLength());
                    }
                }
            }
            idx++;

        }
    }


    private static boolean isInSidingEvaluationTime(List<RotaziunSection> sections, Pair<Integer, Integer> p, IpSolverParams params, boolean sidingIsBeforeSection) {

        int durationToSidingEvaluationTime = mapToPositiveTimeDifference(params.getSidingEvaluationTime() - sections.get(p.getFirst()).getArrival());
        if (sidingIsBeforeSection){
            int emptyTripDuration = getEmptyTripDuration(sections.get(p.getFirst()), sections.get(p.getSecond()), params.getDurationMap());
            durationToSidingEvaluationTime = mapToPositiveTimeDifference(params.getSidingEvaluationTime() - sections.get(p.getFirst()).getArrival() - emptyTripDuration);
        }
        return durationToSidingEvaluationTime < getIdleTime(sections.get(p.getFirst()), sections.get(p.getSecond()), params);

    }

}
