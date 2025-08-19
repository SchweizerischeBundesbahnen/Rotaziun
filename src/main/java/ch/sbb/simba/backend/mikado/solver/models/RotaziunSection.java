package ch.sbb.simba.backend.mikado.solver.models;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RotaziunSection {

    String name;

    @NonNull
    Long id;
    @NonNull
    Long journeyId;
    @NonNull
    Integer departure;
    @NonNull
    Integer arrival;
    @NonNull
    RotaziunStation fromStation;
    @NonNull
    RotaziunStation toStation;
    @NonNull
    List<Integer> debicodes;
    @NonNull
    RotaziunVehicle vehicle;
    @NonNull
    RotaziunSectionType sectionType;
    @NonNull
    Boolean isStamm;

}
