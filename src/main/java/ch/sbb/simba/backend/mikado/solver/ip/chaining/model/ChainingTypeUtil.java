package ch.sbb.simba.backend.mikado.solver.ip.chaining.model;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;
import java.util.List;

public final class ChainingTypeUtil {

    private ChainingTypeUtil() {
    }

    public static ChainingType determineSectionType(RotaziunSection section, ChainingParams chainingParams) {
        if (!section.getIsStamm()) {
            return ChainingType.ENFORCEMENT;
        }
        return section.getArrival() > chainingParams.getDayEveningThreshhold() ? ChainingType.STAMM_EVENING : ChainingType.STAMM_DAY;
    }

    public static boolean isMaintenanceSection(RotaziunSection section) {
        return section.getSectionType() == RotaziunSectionType.DEPOT;
    }

    public static List<RotaziunSection> getNonMaintenanceSections(List<RotaziunSection> sections, IpSolverParams params) {
        return sections.subList(params.getMaintenanceWindows().size(), sections.size());
    }

}
