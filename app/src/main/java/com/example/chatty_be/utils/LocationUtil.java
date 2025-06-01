package com.example.chatty_be.utils;

import java.util.ArrayList;
import java.util.List;

import ch.hsr.geohash.GeoHash;

public class LocationUtil {

    public static String encodeGeohash(double lat, double lon, int precision) {
        return GeoHash.withCharacterPrecision(lat, lon, precision)
                .toBase32();
    }

    public static List<String> getGeohashNeighbors(String geohash) {
        GeoHash center = GeoHash.fromGeohashString(geohash);
        GeoHash[] adjacent = center.getAdjacent();
        List<String> result = new ArrayList<>(adjacent.length);
        for (GeoHash g : adjacent) {
            result.add(g.toBase32());
        }
        return result;
    }

}
