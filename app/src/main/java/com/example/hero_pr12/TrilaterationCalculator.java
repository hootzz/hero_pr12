package com.example.hero_pr12;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrilaterationCalculator {
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
}