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

    public BeaconManager(Activity activity, UIUpdater uiUpdater) {
        this.activity = activity;
        this.uiUpdater = uiUpdater;
        initializeLeScanCallback();
    }

    public void updateOrientationData(float azimuth, float angle, float speed) {
        this.currentAzimuth = azimuth;
        this.currentAngle = angle;
        this.currentSpeed = speed;
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
                        Point estimatedPosition = TrilaterationCalculator.combinedLocalization(strongestBeacons, currentAzimuth, currentAngle, currentSpeed);
                        Log.d("BeaconManager", "Estimated Position: " + estimatedPosition.x + ", " + estimatedPosition.y);
                        uiUpdater.updateLocation(estimatedPosition);
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
        return Math.pow(10, (txPower - rssi) / (10 * 2));
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
        filter.update(new double[] {distance, 0}); // y 좌표는 사용하지 않음
        double[] state = filter.getState();
        return state[0];
    }

    private Map<String, Double> getStrongestBeacons(Map<String, Double> distances, int count) {
        return distances.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(count)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
