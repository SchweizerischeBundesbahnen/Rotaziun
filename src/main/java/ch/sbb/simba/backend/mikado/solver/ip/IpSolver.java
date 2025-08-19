package ch.sbb.simba.backend.mikado.solver.ip;

import static ch.sbb.simba.backend.mikado.solver.parameters.RotaziunResultParams.getResultParams;

import ch.sbb.simba.backend.mikado.solver.ip.chaining.SectionChainUtil;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunResultParams;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPSolver;

import lombok.extern.slf4j.Slf4j;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class IpSolver {

    public static final double INFINITY = Double.POSITIVE_INFINITY;
    public static final int DAY_IN_SECONDS = 24*60*60;

    public static RotaziunResultParams solve(List<RotaziunSection> sections, IpSolverParams params) {

        List<Pair<Integer,Integer>> possibleChains = SectionChainUtil.determinePossibleSectionChains(sections, params);

        Loader.loadNativeLibraries();
        MPSolver solver = selectSolverAndSetSolverParams(params);

        Variables v = new Variables(solver, possibleChains, sections, params);
        Constraints.makeConstraints(solver, possibleChains, v, sections, params);
        Objective.makeObjective(solver, possibleChains, v, sections, params);

        long preSolverTime = System.nanoTime();
        final MPSolver.ResultStatus resultStatus = solver.solve();
        long postSolverTime = System.nanoTime();
        log.info("ComputeTime: " + (postSolverTime - preSolverTime) / 1_000_000_000.0);

        return getResultParams(sections, possibleChains, v, resultStatus, solver, params);
    }

    private static MPSolver selectSolverAndSetSolverParams(IpSolverParams params) {

        MPSolver solver;

        if(params.getStageParams().getStage() == 1 && !params.getStageParams().isOnlyOneBlock()) {

            log.info("SolverType: CBC");
            solver = MPSolver.createSolver("CBC");

        } else {

            log.info("SolverType: CP-SAT");
            solver = MPSolver.createSolver("CP-SAT");

            // to increase performance
            if(!params.getStageParams().isWithMaintenance()){
                // only for non-Maintenance stages
                solver.setSolverSpecificParametersAsString("max_presolve_iterations: 1");
                log.info("LimitPreSolveIterationsToOne: " + true);
            }
            // force CP-SAT solver to parallelize
            String numOfWorkersString = String.format("num_workers: %d; max_time_in_seconds: 90", params.getNumOfWorkers());
            solver.setSolverSpecificParametersAsString(numOfWorkersString);
            log.info("NumOfWorkers: " + params.getNumOfWorkers());

        }

        return solver;

    }

}