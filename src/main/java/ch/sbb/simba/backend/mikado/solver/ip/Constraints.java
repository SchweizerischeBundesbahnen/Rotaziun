package ch.sbb.simba.backend.mikado.solver.ip;

import ch.sbb.simba.backend.mikado.solver.ip.constraints.ConstrBasic;
import ch.sbb.simba.backend.mikado.solver.ip.constraints.ConstrCouplingDecoupling;
import ch.sbb.simba.backend.mikado.solver.ip.constraints.ConstrMaintenance;
import ch.sbb.simba.backend.mikado.solver.ip.constraints.ConstrOneBlock;
import ch.sbb.simba.backend.mikado.solver.ip.constraints.ConstrSidings;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class Constraints {

    private Constraints() {
    }

    public static void makeConstraints(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v,
        List<RotaziunSection> sections, IpSolverParams params) {

        // Basic Constraint
        ConstrBasic.eachSectionIsPartExactlyOnce(solver, possibleChains, v);

        // OneBlock
        if(params.getStageParams().isOnlyOneBlock()){
            ConstrOneBlock.makeSubTourEliminationConstraint(solver, possibleChains, v);
        }

        // Coupling-Decoupling
        if(params.getStageParams().isWithCouplingDecoupling()){
            ConstrCouplingDecoupling.makeCouplingDecouplingConstraints(solver, possibleChains, v, params, sections);
        }

        // Siding
        if(params.getStageParams().isWithSidings()){
            ConstrSidings.makeSidingConstraints(solver, possibleChains, v, sections, params);
        }

        // Mainteinance
        if(params.getStageParams().isWithMaintenance()){
            ConstrMaintenance.makeMaintenanceConstraint(v, params);
        }

    }

}