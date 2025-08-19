package ch.sbb.simba.backend.mikado.solver.blocking.models;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSectionType;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunStation;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RotaziunBlockItem {

    int start;
    int end;
    @NonNull
    RotaziunStation fromStation;
    @NonNull
    RotaziunStation toStation;
    @NonNull
    RotaziunSectionType type;

    RotaziunSection section;

}