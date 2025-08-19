package ch.sbb.simba.backend.mikado.solver.blocking.models;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RotaziunBlockDay {

    int blockingDay;
    @NonNull
    List<RotaziunBlockItem> items;

}
