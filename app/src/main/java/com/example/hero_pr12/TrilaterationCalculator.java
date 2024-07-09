package com.example.hero_pr12;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrilaterationCalculator {

    // 삼변 측량 메소드
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

    // 삼각 측량 메소드
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

    // IWCA 적용 메소드
    public static double adjustDistanceWithIWCA(Point p1, Point p2, Point p3, Point estimatedPoint, double r1, double r2, double r3) {
        double volume = calculateTetrahedronVolume(p1, p2, p3, estimatedPoint);
        return volume / (r1 + r2 + r3);
    }

    // 사면체 부피 계산 메소드
    private static double calculateTetrahedronVolume(Point p1, Point p2, Point p3, Point estimatedPoint) {
        double[][] matrix = {
                {p1.x - estimatedPoint.x, p1.y - estimatedPoint.y, 0},
                {p2.x - estimatedPoint.x, p2.y - estimatedPoint.y, 0},
                {p3.x - estimatedPoint.x, p3.y - estimatedPoint.y, 0}
        };
        double determinant = matrix[0][0] * (matrix[1][1] * matrix[2][2] - matrix[2][1] * matrix[1][2])
                - matrix[0][1] * (matrix[1][0] * matrix[2][2] - matrix[2][0] * matrix[1][2])
                + matrix[0][2] * (matrix[1][0] * matrix[2][1] - matrix[2][0] * matrix[1][1]);
        return Math.abs(determinant / 6.0);
    }

    // RE 알고리즘 적용 메소드
    public static double applyRegressionEstimation(double rssi) {
        // 회귀 분석 모델 적용 (예: y = ax + b)
        double a = -0.1; // 예제 계수
        double b = 0.5;  // 예제 절편
        return a * rssi + b;
    }

    // 결합된 위치 추정 메소드
    public static Point combinedLocalization(Map<String, Double> distances) {
        List<String> beacons = new ArrayList<>(distances.keySet());
        Point p1 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(0));
        Point p2 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(1));
        Point p3 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(2));

        double r1 = distances.get(beacons.get(0));
        double r2 = distances.get(beacons.get(1));
        double r3 = distances.get(beacons.get(2));

        // 삼변 측량
        Point trilaterationPoint = trilateration(distances);

        // 삼각 측량
        Point triangulationPoint = triangulation(p1, p2, p3, r1, r2, r3);

        // IWCA 적용
        double iwcaAdjustedDistance = adjustDistanceWithIWCA(p1, p2, p3, trilaterationPoint, r1, r2, r3);

        // RE 알고리즘 적용
        double reAdjustedDistance = applyRegressionEstimation(iwcaAdjustedDistance);

        // 최종 결합 위치 계산 (단순 평균을 사용)
        double combinedX = (trilaterationPoint.x + triangulationPoint.x) / 2.0;
        double combinedY = (trilaterationPoint.y + triangulationPoint.y) / 2.0;

        return new Point(combinedX, combinedY);
    }
}
