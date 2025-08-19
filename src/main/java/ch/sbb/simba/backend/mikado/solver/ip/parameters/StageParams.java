package ch.sbb.simba.backend.mikado.solver.ip.parameters;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StageParams {

    private int stage;
    private int problemComplexity;

    private Map<RotaziunSection,RotaziunSection> fixedChainMap;

    private boolean onlyOneBlock;
    private boolean withCouplingDecoupling;
    private boolean withSidings;
    private boolean withMaintenance;

}
