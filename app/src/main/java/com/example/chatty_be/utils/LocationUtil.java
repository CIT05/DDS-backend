package com.example.chatty_be.utils;

public class LocationUtil {

    private static final double METERS_PER_DEGREE = 111_000;


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
