package ch.sbb.simba.backend.mikado.solver.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getChainsFromSolution;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;
import static ch.sbb.simba.backend.mikado.solver.ip.Objective.getNumberOfBlockDays;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.hasChainAnEmptyTrip;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isDifferentDebicodes;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isInSidingBetweenJournyes;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isSidingHappening;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.isStammChange;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.isSidingVariableNeeded;

import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlock;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolver.ResultStatus;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Slf4j
@Getter
@Setter
@Builder
public class RotaziunResultParams {

    private Map<RotaziunSection,RotaziunSection> sectionChainMap;
    private Map<RotaziunSection, Boolean> sidingBeforeEmptyTrip;

    private List<RotaziunBlock> blocks;

    private ResultStatus resultStatus;
    private boolean solved;

    private int objectiveValue;
    private int emptyTripDuration;
    private int numOfStammChanges;
    private int numOfBlockDays;
    private int numOfDebicodeChanges;
    private int numOfSidings;
    private int numOfDecoupling;

    public static RotaziunResultParams getResultParams(List<RotaziunSection> sections, List<Pair<Integer, Integer>> possibleChains, Variables v, ResultStatus resultStatus,
        MPSolver solver, IpSolverParams params) {

        Map<RotaziunSection, Boolean> sidingBeforeEmptyTrip = new HashMap<>();

        int emptyTripDuration = 0;
        int numOfStammChanges = 0;
        int numOfBlockDays = 0;
        int numOfDebicodeChanges = 0;
        int numOfSidings = 0;
        int numOfDecouplings = 0;

        for(Pair<Integer,Integer> chain : possibleChains){
            if(v.xMap.get(getKey(chain)).solutionValue() == 1.0){
                int minFromToDuration = ChainAnalysis.getEmptyTripDuration(sections.get(chain.getFirst()), sections.get(chain.getSecond()), params.getDurationMap());
                emptyTripDuration += minFromToDuration;
                numOfStammChanges += isStammChange(sections.get(chain.getFirst()), sections.get(chain.getSecond()), params);
                numOfBlockDays += getNumberOfBlockDays(sections.get(chain.getFirst()), sections.get(chain.getSecond()), minFromToDuration);
                numOfDebicodeChanges += isDifferentDebicodes(sections.get(chain.getFirst()), sections.get(chain.getSecond()), params);
                numOfSidings += isInSidingBetweenJournyes(sections.get(chain.getFirst()), sections.get(chain.getSecond()), params);

                if(params.getStageParams().isWithSidings() && isSidingVariableNeeded(params, sections, chain) && hasChainAnEmptyTrip(sections, chain)){
                    sidingBeforeEmptyTrip.put(sections.get(chain.getFirst()),v.sMap.get(getKey(chain)).getSecond().solutionValue() != 1.0);
                }
            }
        }

        if(params.getStageParams().isWithCouplingDecoupling()){
            numOfDecouplings = (int) Arrays.stream(v.decoup).filter(decoupling -> decoupling.solutionValue() == 1.0).count();
        }

        boolean isSolved = resultStatus == ResultStatus.OPTIMAL || resultStatus == ResultStatus.FEASIBLE;

        RotaziunResultParams result = RotaziunResultParams.builder()
            .sectionChainMap(getChainsFromSolution(sections, possibleChains, v))
            .sidingBeforeEmptyTrip(sidingBeforeEmptyTrip)
            .resultStatus(resultStatus)
            .solved(isSolved)
            .objectiveValue((int) solver.objective().value())
            .emptyTripDuration(emptyTripDuration)
            .numOfStammChanges(numOfStammChanges)
            .numOfBlockDays(numOfBlockDays)
            .numOfDebicodeChanges(numOfDebicodeChanges)
            .numOfSidings(numOfSidings)
            .numOfDecoupling(numOfDecouplings)
            .build();

        logResults(resultStatus, result);
        return result;

    }

    private static void logResults(ResultStatus resultStatus, RotaziunResultParams result) {
        log.info("Result metrics:");

        if (resultStatus == ResultStatus.OPTIMAL) {
            log.info("The solution is optimal.");
        } else if (resultStatus == ResultStatus.FEASIBLE) {
            log.info("The solution is feasible but not optimal.");
        } else {
            log.info("No Solution found.");
        }

        log.info("The cost of the solution is: " + result.getObjectiveValue());
        log.info("Anzahl Leerfahrtminuten: " +  result.getEmptyTripDuration() /60);
        log.info("Anzahl Stammwechsel: " + result.getNumOfStammChanges());
        log.info("Anzahl Umlauftage: " + result.getNumOfBlockDays());
        log.info("Anzahl DebicodeWechsel: " + result.getNumOfDebicodeChanges());
        log.info("Anzahl Abstellungen: " + result.getNumOfSidings());
        log.info("Anzahl Kuppelvorgänge: " + result.getNumOfDecoupling());
    }
}