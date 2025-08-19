package ch.sbb.simba.backend.mikado.solver.ip.chaining;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainingOptionsPercentile.computeChainOptionsPercentiles;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.setChainingParams;

import ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingType;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.FindPossibleChainsMaintenanceStage;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.FindPossibleChains;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.filters.ProhibitedChainFilter;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.filters.SelfLoopFilter;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

@Slf4j
public final class SectionChainUtil {

    private SectionChainUtil() {
    }

    /*
    possible section chains need to consider:
     -> restrictions for SPECIFIC section chains: (fromSection -> toSection)
          -> REQUIRED chains:
              - fixed chains of stage one
              - requiredSectionChains (by User-Input)
          -> PROHIBITED chains:
              - prohibitedSectionChains (by User-Input)
     -> restrictions for GENERAL chaining based on stations and duration
          - prohibitedSidingSites (by User-Input)
          - prohibitedEmptyTrips (by User-Input)
     */

    public static List<Pair<Integer,Integer>> determinePossibleSectionChains(List<RotaziunSection> sections, IpSolverParams params){

        ChainingParams chainingParams = setChainingParams(params);

        List<Pair<Integer,Integer>> possibleChains;
        if(params.getStageParams().isWithMaintenance()){
            possibleChains = FindPossibleChainsMaintenanceStage.getChains(sections, params, chainingParams);
        } else {
            chainingParams.setChainOptionsPercentiles(computeChainOptionsPercentiles(sections, chainingParams));
            logChainOptionsPercentiles(chainingParams);
            possibleChains = FindPossibleChains.getChains(sections, params, chainingParams);
        }

        possibleChains = ProhibitedChainFilter.filter(params, possibleChains);
        return SelfLoopFilter.filter(params, possibleChains);

    }

    private static void logChainOptionsPercentiles(ChainingParams chainingParams) {
        log.info("MinChainOptionsPercentile: " + chainingParams.getMinChainOptionsPercentile());
        log.info("SectionAmountLimit(): " + chainingParams.getSectionAmountLimit());
        log.info("DayEveningThreshhold(): " + chainingParams.getDayEveningThreshhold());

        log.info("percentileStammDay: " + chainingParams.getChainOptionsPercentiles().get(ChainingType.STAMM_DAY));
        log.info("percentileStammEvening: " + chainingParams.getChainOptionsPercentiles().get(ChainingType.STAMM_EVENING));
        log.info("percentileVerstaerker: " + chainingParams.getChainOptionsPercentiles().get(ChainingType.ENFORCEMENT));
    }

}