package ch.sbb.simba.backend.mikado.solver.ip.chaining.filters;

import static java.lang.Math.max;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ChainingOptionsFilter {

    private ChainingOptionsFilter() {
    }

    public static List<RotaziunSection> filterByPercentile(Map<RotaziunSection, Integer> valuedChainTimeMap, int percentile, IpSolverParams params) {
        int timeThreshold = getXthPercentileValue(valuedChainTimeMap.values().stream().toList(), percentile);
        return valuedChainTimeMap.entrySet().stream().filter(e -> e.getValue() <= timeThreshold).map(Entry::getKey).toList();
    }

    static int getXthPercentileValue(List<Integer> values, int percentile) {
        if (percentile < 0 || percentile > 100) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100, currently: " + percentile);
        }
        if (percentile == 100){
            return Collections.max(values);
        }
        List<Integer> sortedValuedTimes = values.stream().sorted().toList();
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        index = max(0, Math.min(index, values.size() - 1));
        return sortedValuedTimes.get(index);
    }

}
