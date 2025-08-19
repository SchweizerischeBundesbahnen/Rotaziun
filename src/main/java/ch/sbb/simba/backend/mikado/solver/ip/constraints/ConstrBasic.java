package ch.sbb.simba.backend.mikado.solver.ip.constraints;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;

import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ConstrBasic {

    private ConstrBasic() {
    }

    // constraint 1: jede kommerzielle Fahrt ist genau ein Mal Teil eines Umlauftages
    public static void eachSectionIsPartExactlyOnce(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        exactlyOneChainAfterEachSection(solver, possibleChains, v);
        exactlyOneChainBeforeEachSection(solver, possibleChains, v);
    }

    // constraint 1a:
    private static void exactlyOneChainAfterEachSection(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        MPConstraint[] constrBasicAfter = new MPConstraint[numOfSections];
        for (int i = 0; i < numOfSections; i++) {
            constrBasicAfter[i] = solver.makeConstraint(1, 1, "basicAfter_" + i);
        }
        for(Pair<Integer,Integer> chain : possibleChains){
            constrBasicAfter[chain.getFirst()].setCoefficient(v.xMap.get(getKey(chain)), 1);
        }
    }

    // constraint 1b:
    private static void exactlyOneChainBeforeEachSection(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        MPConstraint[] constrBasicBefore = new MPConstraint[numOfSections];
        for (int j = 0; j < numOfSections; j++) {
            constrBasicBefore[j] = solver.makeConstraint(1, 1, "basicBefore_" + j);
        }
        for(Pair<Integer,Integer> chain : possibleChains){
            constrBasicBefore[chain.getSecond()].setCoefficient(v.xMap.get(getKey(chain)), 1);
        }
    }

}
