package com.example.hero_pr12;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrilaterationCalculator {

    // Trilateration method
    public static Point trilateration(Map<String, Double> distances) {
        List<String> beacons = new ArrayList<>(distances.keySet());
        Point p1 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(0));
        Point p2 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(1));
        Point p3 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(2));

        double r1 = distances.get(beacons.get(0));
        double r2 = distances.get(beacons.get(1));
        double r3 = distances.get(beacons.get(2));

        double A = 2 * p2.x - 2 * p1.x;
        double B = 2 * p2.y - 2 * p1.y;
        double C = Math.pow(r1, 2) - Math.pow(r2, 2) - Math.pow(p1.x, 2) + Math.pow(p2.x, 2) - Math.pow(p1.y, 2) + Math.pow(p2.y, 2);
        double D = 2 * p3.x - 2 * p2.x;
        double E = 2 * p3.y - 2 * p2.y;
        double F = Math.pow(r2, 2) - Math.pow(r3, 2) - Math.pow(p2.x, 2) + Math.pow(p3.x, 2) - Math.pow(p2.y, 2) + Math.pow(p3.y, 2);

        double x = (C * E - F * B) / (E * A - B * D);
        double y = (C * D - A * F) / (B * D - A * E);

        return new Point(x, y);
    }

    // Triangulation method
    public static Point triangulation(Point p1, Point p2, Point p3, double r1, double r2, double r3) {
        double A = p2.x - p1.x;
        double B = p2.y - p1.y;
        double C = p3.x - p1.x;
        double D = p3.y - p1.y;

        double E = (Math.pow(r1, 2) - Math.pow(r2, 2) + Math.pow(p2.x, 2) - Math.pow(p1.x, 2) + Math.pow(p2.y, 2) - Math.pow(p1.y, 2)) / 2.0;
        double F = (Math.pow(r1, 2) - Math.pow(r3, 2) + Math.pow(p3.x, 2) - Math.pow(p1.x, 2) + Math.pow(p3.y, 2) - Math.pow(p1.y, 2)) / 2.0;

        double x = (E * D - B * F) / (A * D - B * C);
        double y = (A * F - E * C) / (A * D - B * C);

        return new Point(x, y);
    }

    // Combined localization method
    public static Point combinedLocalization(Map<String, Double> distances) {
        List<String> beacons = new ArrayList<>(distances.keySet());
        Point p1 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(0));
        Point p2 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(1));
        Point p3 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(2));

        double r1 = distances.get(beacons.get(0));
        double r2 = distances.get(beacons.get(1));
        double r3 = distances.get(beacons.get(2));

        // Trilateration
        Point trilaterationPoint = trilateration(distances);

        // Triangulation
        Point triangulationPoint = triangulation(p1, p2, p3, r1, r2, r3);

        // Combine results
        double combinedX = (trilaterationPoint.x + triangulationPoint.x) / 2.0;
        double combinedY = (trilaterationPoint.y + triangulationPoint.y) / 2.0;

        return new Point(combinedX, combinedY);
    }
}
