package ch.sbb.simba.backend.mikado.solver.models;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class RotaziunStation {

    String name;

    @NonNull
    Long id;
    @NonNull
    private Float xcoord;
    @NonNull
    private Float ycoord;

}