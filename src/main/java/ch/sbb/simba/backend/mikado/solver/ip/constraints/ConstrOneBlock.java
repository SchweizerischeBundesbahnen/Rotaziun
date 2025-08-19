package ch.sbb.simba.backend.mikado.solver.ip.constraints;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.INFINITY;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;

import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import java.util.Objects;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ConstrOneBlock {

    private ConstrOneBlock() {
    }

    public static void makeSubTourEliminationConstraint(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        firstSectionHasPlacementOne(v);
        allSectionsExceptFirstHavePlacementGreaterOne(v);
        chainedSectionsMustHavePlacementDifferenceOne(solver, possibleChains, v);
    }

    private static void firstSectionHasPlacementOne(Variables v) {
        v.u[0].setBounds(1,1);
    }

    private static void allSectionsExceptFirstHavePlacementGreaterOne(Variables v) {
        for (int i = 1; i < numOfSections; i++) {
            v.u[i].setLb(2);
        }
    }

    private static void chainedSectionsMustHavePlacementDifferenceOne(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v) {
        MPConstraint[] constrOneBlock = new MPConstraint[possibleChains.size()];
        int idx = 0;
        for (Pair<Integer, Integer> chain : possibleChains) {
            if (!Objects.equals(chain.getFirst(), chain.getSecond()) && chain.getFirst() > 0 && chain.getSecond() > 0) {
                constrOneBlock[idx] = solver.makeConstraint(-INFINITY, numOfSections -2, "1Block_" + chain.getFirst() + "_" + chain.getSecond());
                constrOneBlock[idx].setCoefficient(v.xMap.get(getKey(chain)), numOfSections -1);
                constrOneBlock[idx].setCoefficient(v.u[chain.getFirst()], 1);
                constrOneBlock[idx].setCoefficient(v.u[chain.getSecond()], -1);
            }
            idx++;
        }
    }

}
