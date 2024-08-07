package com.example.hero_pr12;

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
        uiUpdater.setBeaconPositions(BeaconInfoLoader.BEACON_LOCATIONS); // 비콘 위치 초기화
    }

    public void updateOrientationData(float azimuth, float angle, float speed) {
        this.currentAzimuth = azimuth;
        this.currentAngle = angle;
        this.currentSpeed = speed;
        uiUpdater.updateUserOrientation(azimuth);
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

    public Point getCurrentUserPosition() {
        return currentUserPosition;
    }

    public void updateUserPosition(Point newPosition) {
        this.currentUserPosition = newPosition;
        uiUpdater.updateLocation(currentUserPosition);
    }

    private void initializeLeScanCallback() {
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("BeaconManager", "Beacon detected: " + device.getAddress());
                String beaconId = BeaconInfoLoader.beaconMacAddress.get(device.getAddress());
                if (beaconId != null) {
                    double weightedAverageRssi = calculateWeightedAverageRssi(beaconId, rssi);
                    float distance = BeaconDistanceCalculator.calculateDistance((float) weightedAverageRssi, BeaconDistanceCalculator.CALIBRATED_RSSI_AT_ONE_METER, BeaconDistanceCalculator.PATH_LOSS_PARAMETER_INDOOR);
                    double filteredDistance = applyDoubleKalmanFilter(beaconId, distance);
                    distances.put(beaconId, filteredDistance);
                    uiUpdater.updateInfoTextView(distances);
                    if (distances.size() >= 3) {
                        Map<String, Double> strongestBeacons = getStrongestBeacons(distances, 3);
                        Point estimatedPosition = calculatePosition(strongestBeacons);
                        Log.d("BeaconManager", "Estimated Position: " + estimatedPosition.x + ", " + estimatedPosition.y);
                        updateUserPosition(estimatedPosition);
                    }
                }
            }
        };
    }

    public void startBeaconScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("BeaconManager", "Starting Beacon Scan");
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    private double calculateWeightedAverageRssi(String beaconId, int rssi) {
        List<Integer> values = rssiValues.getOrDefault(beaconId, new ArrayList<>());
        if (values.size() >= RSSI_FILTER_SIZE) {
            values.remove(0);
        }
        values.add(rssi);
        rssiValues.put(beaconId, values);

        double weightedSum = 0;
        double weightSum = 0;
        double weight = 1.0;
        for (int value : values) {
            weightedSum += value * weight;
            weightSum += weight;
            weight *= 0.9; // 가중치를 점차 감소시킴
        }
        return weightedSum / weightSum;
    }

    private double applyDoubleKalmanFilter(String beaconId, float distance) {
        ExtendedKalmanFilter filter = kalmanFilters.get(beaconId);
        if (filter == null) {
            filter = new ExtendedKalmanFilter();
            kalmanFilters.put(beaconId, filter);
        }
        double[] control = {0, 0};
        filter.predict(control);
        filter.update(new double[]{distance, 0});
        filter.predict(control);
        filter.update(new double[]{distance, 0});
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

        Point trilaterationPoint = TrilaterationCalculator.trilateration(distances);
        Point triangulationPoint = TrilaterationCalculator.triangulation(p1, p2, p3, r1, r2, r3);

        double combinedX = (trilaterationPoint.x + triangulationPoint.x) / 2.0;
        double combinedY = (trilaterationPoint.y + triangulationPoint.y) / 2.0;

        combinedX += Math.cos(Math.toRadians(currentAzimuth)) * currentSpeed;
        combinedY += Math.sin(Math.toRadians(currentAzimuth)) * currentSpeed;

        return new Point(combinedX, combinedY);
    }
}
