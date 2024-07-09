package com.example.hero_pr12;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import android.content.pm.PackageManager;

public class BeaconManager {
    private static final int RSSI_FILTER_SIZE = 10;
    private final Activity activity;
    private final UIUpdater uiUpdater;
    private final Map<String, List<Integer>> rssiValues = new HashMap<>();
    private final Map<String, ExtendedKalmanFilter> kalmanFilters = new HashMap<>();
    private final Map<String, Double> distances = new HashMap<>();
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private float currentAzimuth;
    private float currentAngle;
    private float currentSpeed;
    private Point currentUserPosition = new Point(0, 0);

    public BeaconManager(Activity activity, UIUpdater uiUpdater) {
        this.activity = activity;
        this.uiUpdater = uiUpdater;
        initializeLeScanCallback();
    }

    public void updateOrientationData(float azimuth, float angle, float speed) {
        this.currentAzimuth = azimuth;
        this.currentAngle = angle;
        this.currentSpeed = speed;
        uiUpdater.updateUserOrientation(azimuth); // MapView에서 사용자 방향을 업데이트
    }

    public float getCurrentAzimuth() {
        return currentAzimuth;
    }

    public float getCurrentAngle() {
        return currentAngle;
    }

    public float getCurrentSpeed() {
        return currentSpeed;
    }

    public void updateUserPosition(double deltaX, double deltaY) {
        currentUserPosition.x += deltaX;
        currentUserPosition.y += deltaY;
        uiUpdater.updateLocation(currentUserPosition);
    }

    private void initializeLeScanCallback() {
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("BeaconManager", "Beacon detected: " + device.getAddress());
                String beaconId = BeaconInfoLoader.beaconMacAddress.get(device.getAddress());
                if (beaconId != null) {
                    double filteredRssi = calculateFilteredRssi(beaconId, rssi);
                    double distance = calculateDistance(beaconId, filteredRssi);
                    double filteredDistance = applyKalmanFilter(beaconId, distance);
                    distances.put(beaconId, filteredDistance);
                    uiUpdater.updateInfoTextView(distances);
                    if (distances.size() >= 3) {
                        Map<String, Double> strongestBeacons = getStrongestBeacons(distances, 3);
                        Point estimatedPosition = calculatePosition(strongestBeacons);
                        Log.d("BeaconManager", "Estimated Position: " + estimatedPosition.x + ", " + estimatedPosition.y);
                        updateUserPosition(estimatedPosition.x - currentUserPosition.x, estimatedPosition.y - currentUserPosition.y);
                    }
                }
            }
        };
    }

    public void startBeaconScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("BeaconManager", "Starting Beacon Scan");
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    private double calculateFilteredRssi(String beaconId, int rssi) {
        List<Integer> values = rssiValues.getOrDefault(beaconId, new ArrayList<>());
        if (values.size() >= RSSI_FILTER_SIZE) {
            values.remove(0);
        }
        values.add(rssi);
        rssiValues.put(beaconId, values);

        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return sum / (double) values.size();
    }

    private double calculateDistance(String beaconId, double rssi) {
        int txPower = BeaconInfoLoader.beaconTxPower.getOrDefault(beaconId, -59);
        double n = 2.0; // 환경에 따른 패스 로스 계수 (필요에 따라 조정)
        return Math.pow(10, (txPower - rssi) / (10 * n));
    }

    private double applyKalmanFilter(String beaconId, double distance) {
        ExtendedKalmanFilter filter = kalmanFilters.get(beaconId);
        if (filter == null) {
            filter = new ExtendedKalmanFilter();
            kalmanFilters.put(beaconId, filter);
        }
        // 측정값으로 거리, 제어 입력은 0으로 가정
        double[] control = {0, 0}; // 제어 입력이 없을 경우
        filter.predict(control);
        filter.update(new double[]{distance, 0}); // y 좌표는 사용하지 않음
        double[] state = filter.getState();
        return state[0];
    }

    private Map<String, Double> getStrongestBeacons(Map<String, Double> distances, int count) {
        return distances.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(count)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Point calculatePosition(Map<String, Double> distances) {
        List<String> beacons = new ArrayList<>(distances.keySet());
        Point p1 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(0));
        Point p2 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(1));
        Point p3 = BeaconInfoLoader.BEACON_LOCATIONS.get(beacons.get(2));

        double r1 = distances.get(beacons.get(0));
        double r2 = distances.get(beacons.get(1));
        double r3 = distances.get(beacons.get(2));

        // 사용자 방향 보정
        double angle1 = Math.atan2(p1.y - currentUserPosition.y, p1.x - currentUserPosition.x) - Math.toRadians(currentAzimuth);
        double angle2 = Math.atan2(p2.y - currentUserPosition.y, p2.x - currentUserPosition.x) - Math.toRadians(currentAzimuth);
        double angle3 = Math.atan2(p3.y - currentUserPosition.y, p3.x - currentUserPosition.x) - Math.toRadians(currentAzimuth);

        Point adjustedP1 = new Point(p1.x + r1 * Math.cos(angle1), p1.y + r1 * Math.sin(angle1));
        Point adjustedP2 = new Point(p2.x + r2 * Math.cos(angle2), p2.y + r2 * Math.sin(angle2));
        Point adjustedP3 = new Point(p3.x + r3 * Math.cos(angle3), p3.y + r3 * Math.sin(angle3));

        // 삼변 측량과 삼각 측량 결합
        Point trilaterationPoint = TrilaterationCalculator.trilateration(distances);
        Point triangulationPoint = TrilaterationCalculator.triangulation(adjustedP1, adjustedP2, adjustedP3, r1, r2, r3);

        // 결합 위치 계산 (단순 평균)
        double combinedX = (trilaterationPoint.x + triangulationPoint.x) / 2.0;
        double combinedY = (trilaterationPoint.y + triangulationPoint.y) / 2.0;

        // 사용자 이동 방향과 거리 보정
        combinedX += Math.cos(Math.toRadians(currentAzimuth)) * currentSpeed;
        combinedY += Math.sin(Math.toRadians(currentAzimuth)) * currentSpeed;

        return new Point(combinedX, combinedY);
    }
}
