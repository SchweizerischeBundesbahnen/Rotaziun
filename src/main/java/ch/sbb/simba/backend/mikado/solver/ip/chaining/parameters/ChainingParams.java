package ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.mapToPositiveTimeDifference;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.isMaintenanceSection;

import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class ChainingParams {

    public static final int MAX_TIME_DIFFERENCE_FOR_FIXED_LINK = 90 * 60;

    public static final double EMPTY_TRIP_MULTIPLIER = 4;
    public static final int STAMM_CHANGE_ADDITION = 90 * 60; // [s]
    public static final double RATIO_EVENING_DAY_CHAINOPTIONS_MAX = 2.5;
    public static final double RATIO_ENFORCEMENT_STAMM_CHAINOPTIONS_MAX = 5;

    private static final int[][] MIN_CHAIN_OPTIONS_PERCENTILE_MATRIX = {{7,12,20},{6,7,12},{5,6,10},{4,5,8}};
    private static final int[][] DAY_EVENING_THRESHHOLD_MATRIX = {{17*3600,16*3600,15*3600},{18*3600,17*3600,16*3600},{19*3600,18*3600,17*3600},{20*3600,19*3600,18*3600}};
    private static final int[][] SECTION_AMOUNT_LIMIT_MATRIX = {{100,180,220},{35,60,90},{30,50,80},{30,40,60}};

    private int minChainOptionsPercentile;
    private int dayEveningThreshhold;
    private int sectionAmountLimit;

    private Map<RotaziunSection,RotaziunSection> fixedChainMap;
    private Set<RotaziunSection> sectionsWithOutgoingFixedChain;
    private Set<RotaziunSection> sectionsWithIncomingFixedChain;

    private EnumMap<ChainingType, Integer> chainOptionsPercentiles;

    // Maintenance Stage
    public static final int[] MAINTENANCE_CHAIN_OPTIONS_PERCENTILE = {50,75,100};

    public static final int LARGE_GAP_TIME_THRESHOLD = 5 * 3600;
    private List<RotaziunSection> startOfGapSections;
    private List<RotaziunSection> endOfGapSections;


    public static ChainingParams setChainingParams(IpSolverParams params) {

        Map<RotaziunSection, RotaziunSection> fixedChains = new HashMap<>();

        if(!params.getStageParams().isWithMaintenance()){
            // add required specific section chains by stage one
            fixedChains.putAll(params.getStageParams().getFixedChainMap());
        }
        // required specific section chains by user-input
        params.getRequiredSectionChains().forEach(pair -> fixedChains.put(pair.getFirst(),pair.getSecond()));

        // gap sections
        List<RotaziunSection> startOfGapSections = new ArrayList<>();
        List<RotaziunSection> endOfGapSections = new ArrayList<>();
        if(params.getStageParams().isWithMaintenance()){
            computeLargeGapSections(params,startOfGapSections,endOfGapSections);
        }

        return ChainingParams.builder()
            .minChainOptionsPercentile(MIN_CHAIN_OPTIONS_PERCENTILE_MATRIX[params.getStageParams().getProblemComplexity()][params.getPrecisionLevel()])
            .dayEveningThreshhold(DAY_EVENING_THRESHHOLD_MATRIX[params.getStageParams().getProblemComplexity()][params.getPrecisionLevel()])
            .sectionAmountLimit(SECTION_AMOUNT_LIMIT_MATRIX[params.getStageParams().getProblemComplexity()][params.getPrecisionLevel()])
            .fixedChainMap(fixedChains)
            .sectionsWithOutgoingFixedChain(new HashSet<>(fixedChains.keySet()))
            .sectionsWithIncomingFixedChain(new HashSet<>(fixedChains.values()))
            .startOfGapSections(startOfGapSections)
            .endOfGapSections(endOfGapSections)
            .build();
    }

    private static void computeLargeGapSections(IpSolverParams params, List<RotaziunSection> startOfGapSections, List<RotaziunSection> endOfGapSections) {
        for (Map.Entry<RotaziunSection, RotaziunSection> entry : params.getStageParams().getFixedChainMap().entrySet()) {
            if(isLargeGap(entry.getKey(), entry.getValue())){
                if(!isMaintenanceSection(entry.getKey())){
                    startOfGapSections.add(entry.getKey());
                }
                if(!isMaintenanceSection(entry.getValue())){
                    endOfGapSections.add(entry.getValue());
                }
            }
        }
    }

    private static boolean isLargeGap(RotaziunSection fromSection, RotaziunSection toSection) {
        return mapToPositiveTimeDifference(toSection.getDeparture() - fromSection.getArrival()) > LARGE_GAP_TIME_THRESHOLD;
    }

}