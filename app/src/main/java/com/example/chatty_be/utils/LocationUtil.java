package com.example.chatty_be.utils;

import java.util.ArrayList;
import java.util.List;

import ch.hsr.geohash.GeoHash;

public class LocationUtil {

    private static final double METERS_PER_DEGREE = 111_000;

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

    public static double roundLocation(double coordinates, double precisionMeters) {
        double precisionDegrees = precisionMeters / METERS_PER_DEGREE;
        return Math.round(coordinates / precisionDegrees) * precisionDegrees;
    }

    public static MinMaxCoordinates getMinMaxCoordinatesBox(double centerLat, double centerLon, double radiusMeters) {

        double radiusLatDegrees = radiusMeters / METERS_PER_DEGREE;

        double metersPerDegreeLon = METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat));
        double radiusLonDegrees = radiusMeters / metersPerDegreeLon;

        return new MinMaxCoordinates(
                centerLat - radiusLatDegrees,
                centerLat + radiusLatDegrees,
                centerLon - radiusLonDegrees,
                centerLon + radiusLonDegrees
        );
    }

    public static boolean isCoordinatesInCircle(double lat, double lon, double centerLat, double centerLon, double radiusMeters) {
        return Math.sqrt(
                Math.pow((lat - centerLat) * METERS_PER_DEGREE, 2) +
                Math.pow((lon - centerLon) * METERS_PER_DEGREE * Math.cos(Math.toRadians(centerLat)), 2)
        ) <= radiusMeters;
    }


    public static class MinMaxCoordinates {
        public final double minLat, maxLat, minLon, maxLon;

        public MinMaxCoordinates(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }
    }



}
