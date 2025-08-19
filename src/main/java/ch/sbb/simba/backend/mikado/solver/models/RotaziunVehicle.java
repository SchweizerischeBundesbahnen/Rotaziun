package ch.sbb.simba.backend.mikado.solver.models;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RotaziunVehicle {

    String name;

    @NonNull
    Long id;
    @NonNull
    float length;

}