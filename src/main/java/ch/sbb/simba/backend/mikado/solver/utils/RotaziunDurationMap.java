package ch.sbb.simba.backend.mikado.solver.utils;

import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunStation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;

@Getter
public class RotaziunDurationMap {

    private final Map<Relation, Integer> minFromToDuration = new HashMap<>();
    private final Map<Long, RotaziunStation> stationsById = new HashMap<>();;

    record Relation(Long fromId, Long toId) {

    }

    public RotaziunDurationMap(List<RotaziunSection> sections) {

        // fill stationByIdMap
        getAllStations(sections).forEach(station -> stationsById.put(station.getId(), station));

        // save all section duration times
        for (RotaziunSection section : sections) {
            saveInMap(section.getFromStation().getId(), section.getToStation().getId(), section.getArrival() - section.getDeparture());
        }

        // compute remaining relation times
        addEmptyTripTimes(getAllStations(sections));
    }


    private static List<RotaziunStation> getAllStations(List<RotaziunSection> sections) {
        return sections.stream().flatMap(section -> Stream.of(section.getToStation(), section.getFromStation())).distinct().toList();
    }

    private void saveInMap(Long fromStationId, Long toStationId, int duration) {
        var relation = new Relation(fromStationId, toStationId);
        if (this.minFromToDuration.getOrDefault(relation, Integer.MAX_VALUE) > duration) {
            this.minFromToDuration.put(relation, duration);
        }
    }

    private void addEmptyTripTimes(List<RotaziunStation> allStations) {
        for(RotaziunStation stationFrom : allStations){
            for(RotaziunStation stationTo : allStations){
                getDuration(stationFrom.getId(), stationTo.getId());
            }
        }
    }

    public Integer getDuration(Long fromStationId, Long toStationId) {
        var relation = new Relation(fromStationId, toStationId);
        if (!minFromToDuration.containsKey(relation)) {
            minFromToDuration.put(relation, comupteAdditionalDuration(fromStationId, toStationId));
        }
        var x = minFromToDuration.get(relation);
        return Math.min(x,DAY_IN_SECONDS-3600);
    }

    private Integer comupteAdditionalDuration(Long fromStationId, Long toStationId) {
        // if a reverse duration exists, use it

        var reverseRelation = new Relation(toStationId, fromStationId);
        if (this.minFromToDuration.containsKey(reverseRelation)) {
            return this.minFromToDuration.get(reverseRelation);
        }

        var from = stationsById.get(fromStationId);
        var to = stationsById.get(toStationId);

        var distance = computeBeeLineDistance(from, to);
        var speed = computeSpeed(distance);
        return Long.valueOf(Math.round(distance / speed * 3600)).intValue();
    }

    private double computeBeeLineDistance(RotaziunStation from, RotaziunStation to) {

        double lon1 = Math.toRadians(from.getXcoord());
        double lat1 = Math.toRadians(from.getYcoord());
        double lon2 = Math.toRadians(to.getXcoord());
        double lat2 = Math.toRadians(to.getYcoord());

        double earthRadius = 6371; // Radius of the Earth in kilometers

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.sin(dlon / 2) * Math.sin(dlon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c;
    }

    private int computeSpeed(double distance) {
        // Boundaries for speed interpolation
        int minDistance = 30;
        int maxDistance = 400;
        int minSpeed = 30;
        int maxSpeed = 180;

        if (distance <= minDistance) {
            return minSpeed;
        } else if (distance >= maxDistance) {
            return maxSpeed;
        } else {
            // Linear interpolation
            return (int) (minSpeed + (maxSpeed - minSpeed) * ((distance - minDistance) / (maxDistance - minDistance)));
        }
    }

}