package ch.sbb.simba.backend.mikado.solver.ip.chaining.filters;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;

import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.List;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class SelfLoopFilter {

    private SelfLoopFilter() {
    }

    public static List<Pair<Integer, Integer>> filter(IpSolverParams params, List<Pair<Integer, Integer>> chains) {

        if (numOfSections <= 1) {
            return chains;
        }

        return chains.stream().filter(chain -> !chain.getFirst().equals(chain.getSecond())).toList();
    }

}