package ch.sbb.simba.backend.mikado.solver.ip.constraints;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;

import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ConstrMaintenance {

    private static final int MIN_ABSOLUTE_TOLERANCE = 2;

    private ConstrMaintenance() {
    }

    public static void makeMaintenanceConstraint(Variables v, IpSolverParams params) {

        int maintenanceWindowCount = params.getMaintenanceWindows().size();
        int tol = computeToleranceForPlacementBound(params, maintenanceWindowCount);
        log.info("Equal Spacing: " + numOfSections/params.getMaintenanceWindows().size());
        log.info("Spacing Tolerance: " + tol);

        for (int i=1; i<params.getMaintenanceWindows().size(); i++) {
            v.u[i].setBounds((int) ((double) numOfSections /maintenanceWindowCount)*i - tol,(int) ((double) numOfSections /maintenanceWindowCount)*i + tol);
        }

    }

    private static int computeToleranceForPlacementBound(IpSolverParams params, int maintenanceWindowCount) {
        return Math.max(MIN_ABSOLUTE_TOLERANCE,(int) (((double) numOfSections /maintenanceWindowCount)*params.getMaintenanceWindowDistributionTolerance()));
    }

}