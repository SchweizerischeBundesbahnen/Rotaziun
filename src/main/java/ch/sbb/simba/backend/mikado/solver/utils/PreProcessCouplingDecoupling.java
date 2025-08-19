package ch.sbb.simba.backend.mikado.solver.utils;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class PreProcessCouplingDecoupling {

    private PreProcessCouplingDecoupling() {
    }

    // finds enforcement-stamm-pairs by equal journey id
    public static Map<RotaziunSection, RotaziunSection> findEnforcementStammPairs(List<RotaziunSection> sections) {
        Map<RotaziunSection,RotaziunSection> enforcementStammPairMap = new HashMap<>();
        List<RotaziunSection> stammList = sections.stream().filter(RotaziunSection::getIsStamm).toList();
        List<RotaziunSection> enforcementList = sections.stream().filter(s -> !s.getIsStamm()).toList();
        for(RotaziunSection e : enforcementList) {
            enforcementStammPairMap.put(e, stammList.stream().filter(s -> Objects.equals(s.getJourneyId(), e.getJourneyId())).findFirst().orElse(null));
        }
        enforcementStammPairMap.entrySet().removeIf(entry -> entry.getValue() == null);
        return enforcementStammPairMap;
    }

    public static Map<RotaziunSection, Integer> makeEnforcementToIdMap(Map<RotaziunSection, RotaziunSection> enforcementStammPairMap) {
        List<RotaziunSection> keys = new ArrayList<>(enforcementStammPairMap.keySet());
        return IntStream.range(0, keys.size()).boxed().collect(Collectors.toMap(keys::get, i -> i));
    }

    public static Map<Long, Integer> makeSectionToCouplingDecouplingMap(List<RotaziunSection> sections, boolean isCoupling) {
        Map<String, Integer> keyToCoupDecoupIdMap = new HashMap<>(); // (key -> Coup/Decoup-ID)
        Map<Long, Integer> sectionIdToCoupDecoupIdMap = new HashMap<>(); // (section-ID -> Coup/Decoup-ID)
        AtomicInteger counter = new AtomicInteger(1);

        for (RotaziunSection section : sections) {
            String key = buildKey(section, isCoupling); // key = id + (from/to)station
            // If key is already seen, use its number; otherwise, assign a new number.
            int coupDecoupId = keyToCoupDecoupIdMap.computeIfAbsent(key, k -> counter.getAndIncrement());
            sectionIdToCoupDecoupIdMap.put(section.getId(), coupDecoupId);
        }

        return sectionIdToCoupDecoupIdMap;
    }

    // Build a key combining the journeyId and the station id based on the isCoupling flag
    private static String buildKey(RotaziunSection section, boolean isCoupling) {
        long stationId = isCoupling ? section.getToStation().getId() : section.getFromStation().getId();
        return section.getJourneyId() + ":" + stationId;
    }

}
