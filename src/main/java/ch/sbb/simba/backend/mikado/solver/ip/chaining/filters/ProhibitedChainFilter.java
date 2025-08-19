package ch.sbb.simba.backend.mikado.solver.ip.chaining.filters;

import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ProhibitedChainFilter {

    private ProhibitedChainFilter() {
    }

    // remove User-Defined prohibited section chains
    public static List<Pair<Integer, Integer>> filter (IpSolverParams params, List<Pair<Integer, Integer>> chains) {

        Set<Pair<Integer,Integer>> prohibitedChainIndices = params.getProhibitedSectionChains().stream()
            .map(chain -> new Pair<>(params.getSectionIdxMap().get(chain.getFirst()), params.getSectionIdxMap().get(chain.getSecond())))
            .collect(Collectors.toSet());
        return chains.stream().filter(chain -> !prohibitedChainIndices.contains(chain)).toList();

    }

}
