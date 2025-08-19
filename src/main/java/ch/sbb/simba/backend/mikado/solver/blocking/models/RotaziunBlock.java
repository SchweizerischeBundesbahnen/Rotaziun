package ch.sbb.simba.backend.mikado.solver.blocking.models;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
public class RotaziunBlock {

    @NonNull
    List<RotaziunBlockDay> days;

}
