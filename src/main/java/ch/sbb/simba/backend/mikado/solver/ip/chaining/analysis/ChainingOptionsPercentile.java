package ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.RATIO_ENFORCEMENT_STAMM_CHAINOPTIONS_MAX;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams.RATIO_EVENING_DAY_CHAINOPTIONS_MAX;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingType;
import ch.sbb.simba.backend.mikado.solver.ip.chaining.parameters.ChainingParams;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ChainingOptionsPercentile {

    private ChainingOptionsPercentile() {
    }

    // Percentile is between 0 and 100
    public static EnumMap<ChainingType, Integer> computeChainOptionsPercentiles(List<RotaziunSection> sections, ChainingParams chainingParams) {

        if(numOfSections - chainingParams.getFixedChainMap().size() < chainingParams.getSectionAmountLimit()){
            // all chaining options are allowed -> return 100th percentile for all catergories
            return getFullPercentilesForUnrestrictedChaining();
        }

        return computePercentilesForRestictedChaining(sections, chainingParams);

    }

    private static EnumMap<ChainingType, Integer> computePercentilesForRestictedChaining(List<RotaziunSection> sections, ChainingParams chainingParams) {

        List<RotaziunSection> unchainedFromSections = getUnchainedFromSections(sections, chainingParams);
        int n = unchainedFromSections.size();

        int enforcementsCount = getEnforcementsCount(unchainedFromSections);
        int stammDayCount = getStammDayCount(chainingParams, unchainedFromSections);
        int stammEveningCount = getStammEveningCount(n, enforcementsCount, stammDayCount);

        int maxChainsToRemove = calculatMaxAmountOfChainsToRemove(n, stammDayCount, stammEveningCount, enforcementsCount, chainingParams);
        int targetCountOfChainsToRemove = calculateTargetCountOfChainsToRemove(chainingParams, n);

        if(targetCountOfChainsToRemove > maxChainsToRemove){
            return getMinimalPercentilesForChaining(chainingParams);
        }

        return getScaledPercentilesForChaining(chainingParams, n, targetCountOfChainsToRemove, stammDayCount, stammEveningCount, enforcementsCount);
    }

    private static EnumMap<ChainingType, Integer> getFullPercentilesForUnrestrictedChaining() {
        return new EnumMap<>(Map.of(
            ChainingType.STAMM_DAY, 100,
            ChainingType.STAMM_EVENING, 100,
            ChainingType.ENFORCEMENT, 100
        ));
    }

    private static EnumMap<ChainingType, Integer> getMinimalPercentilesForChaining(ChainingParams chainingParams) {
        return new EnumMap<>(Map.of(
            ChainingType.STAMM_DAY, chainingParams.getMinChainOptionsPercentile(),
            ChainingType.STAMM_EVENING, (int) (RATIO_EVENING_DAY_CHAINOPTIONS_MAX * chainingParams.getMinChainOptionsPercentile()),
            ChainingType.ENFORCEMENT, (int) (RATIO_ENFORCEMENT_STAMM_CHAINOPTIONS_MAX * chainingParams.getMinChainOptionsPercentile())
        ));
    }

    private static EnumMap<ChainingType, Integer> getScaledPercentilesForChaining(ChainingParams chainingParams, int n, double targetCountOfChainsToRemove, int stammDayCount, int stammEveningCount,
        int enforcementsCount) {

        int l0Max = (int) (n * (1 - (chainingParams.getMinChainOptionsPercentile() * 0.01)));
        int l1Max = (int) (n * (1 - RATIO_EVENING_DAY_CHAINOPTIONS_MAX * (chainingParams.getMinChainOptionsPercentile() * 0.01)));
        int l2Max = (int) (n * (1 - RATIO_ENFORCEMENT_STAMM_CHAINOPTIONS_MAX * (chainingParams.getMinChainOptionsPercentile() * 0.01)));

        double scalingFactor = targetCountOfChainsToRemove / (stammDayCount * l0Max + stammEveningCount * l1Max + enforcementsCount * l2Max);

        return new EnumMap<>(Map.of(
            ChainingType.STAMM_DAY, (int) ((n - scalingFactor * l0Max) * 100 / n),
            ChainingType.STAMM_EVENING, (int) ((n - scalingFactor * l1Max) * 100 / n),
            ChainingType.ENFORCEMENT, (int) ((n - scalingFactor * l2Max) * 100 / n)
        ));
    }

    private static List<RotaziunSection> getUnchainedFromSections(List<RotaziunSection> sections, ChainingParams chainingParams) {
        return IntStream.range(0, numOfSections).filter(i -> !chainingParams.getSectionsWithOutgoingFixedChain().contains(sections.get(i))).mapToObj(sections::get).toList();
    }

    private static int getEnforcementsCount(List<RotaziunSection> unchainedFromSections) {
        return (int) unchainedFromSections.stream().filter(s -> !s.getIsStamm()).distinct().count();
    }

    private static int getStammDayCount(ChainingParams chainingParams, List<RotaziunSection> unchainedFromSections) {
        return (int) unchainedFromSections.stream().filter(RotaziunSection::getIsStamm)
            .filter(s -> s.getArrival() < chainingParams.getDayEveningThreshhold()).distinct().count();
    }

    private static int getStammEveningCount(int n, int enforcementsCount, int stammDayCount) {
        return n - enforcementsCount - stammDayCount;
    }

    private static int calculatMaxAmountOfChainsToRemove(int n, int numOfStammDay, int numOfStammEvening, int numOfEnforcements, ChainingParams chainingParams) {
        return (int) (n * n - (n * (chainingParams.getMinChainOptionsPercentile() * 0.01) * (numOfStammDay + numOfStammEvening * RATIO_EVENING_DAY_CHAINOPTIONS_MAX
            + numOfEnforcements * RATIO_ENFORCEMENT_STAMM_CHAINOPTIONS_MAX)));
    }

    private static int calculateTargetCountOfChainsToRemove(ChainingParams chainingParams, int n) {
        return n * n - chainingParams.getSectionAmountLimit() * chainingParams.getSectionAmountLimit();
    }

}