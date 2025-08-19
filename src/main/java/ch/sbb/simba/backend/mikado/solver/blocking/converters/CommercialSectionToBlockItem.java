package ch.sbb.simba.backend.mikado.solver.blocking.converters;

import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlockItem;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;

public final class CommercialSectionToBlockItem {

    private CommercialSectionToBlockItem() {
    }

    public static RotaziunBlockItem convert(RotaziunSection commercialSection) {

        int start = commercialSection.getDeparture() % DAY_IN_SECONDS;
        int duration = commercialSection.getArrival() - commercialSection.getDeparture();

        return RotaziunBlockItem.builder()
            .start(start)
            .end(start + duration)
            .fromStation(commercialSection.getFromStation())
            .toStation(commercialSection.getToStation())
            .section(commercialSection)
            .type(RotaziunSectionType.COMMERCIAL)
            .build();
    }

}
