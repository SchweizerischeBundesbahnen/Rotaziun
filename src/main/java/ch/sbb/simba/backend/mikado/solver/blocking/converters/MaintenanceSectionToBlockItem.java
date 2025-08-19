package ch.sbb.simba.backend.mikado.solver.blocking.converters;

import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlockItem;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;

public final class MaintenanceSectionToBlockItem {

    private MaintenanceSectionToBlockItem() {
    }

    public static RotaziunBlockItem convert(RotaziunSection maintenanceSection) {

        return RotaziunBlockItem.builder()
            .start(maintenanceSection.getDeparture())
            .end(maintenanceSection.getArrival())
            .fromStation(maintenanceSection.getFromStation())
            .toStation(maintenanceSection.getToStation())
            .type(RotaziunSectionType.DEPOT)
            .build();
    }

}
