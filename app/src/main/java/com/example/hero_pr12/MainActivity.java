package com.example.hero_pr12;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 2;
    private static final Map<String, Point> BEACON_LOCATIONS = new HashMap<String, Point>() {{
        put("BEACON_1", new Point(0, 0));
        put("BEACON_2", new Point(10, 0));
        put("BEACON_3", new Point(5, 10));
    }};

    private static final int RSSI_FILTER_SIZE = 10;
    private Map<String, List<Integer>> rssiValues = new HashMap<>();
    private Map<String, KalmanFilter> kalmanFilters = new HashMap<>();
    private Map<String, Double> distances = new HashMap<>();
    private MapView mapView;
    private TextView infoTextView;
    private BluetoothAdapter.LeScanCallback leScanCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        infoTextView = findViewById(R.id.infoTextView);
        Log.d("MainActivity", "onCreate called");

        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            checkBluetoothPermissions();
        }

        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("MainActivity", "Beacon detected: " + device.getAddress());
                String beaconId = getBeaconId(scanRecord);
                if (BEACON_LOCATIONS.containsKey(beaconId)) {
                    double filteredRssi = calculateFilteredRssi(beaconId, rssi);
                    double distance = calculateDistance(filteredRssi);
                    double filteredDistance = applyKalmanFilter(beaconId, distance);
                    distances.put(beaconId, filteredDistance);
                    if (distances.size() >= 3) {
                        Point estimatedPosition = trilateration(distances);
                        updateLocation(estimatedPosition);
                        updateInfoTextView(estimatedPosition);
                    }
                }
            }
        };
    }

    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH);
        } else {
            startBeaconScan();
        }
    }

    private void startBeaconScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    private String getBeaconId(byte[] scanRecord) {
        // 비콘 ID를 추출하는 로직 (UUID, Major, Minor 조합)
        return "BEACON_1"; // 예제용 하드코딩 값
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

    private double calculateDistance(double rssi) {
        int txPower = -59; // 비콘의 발신 신호 세기 (dBm)
        return Math.pow(10, (txPower - rssi) / (10 * 2));
    }

    private double applyKalmanFilter(String beaconId, double distance) {
        KalmanFilter filter = kalmanFilters.get(beaconId);
        if (filter == null) {
            filter = new KalmanFilter();
            kalmanFilters.put(beaconId, filter);
        }
        return filter.filter(distance);
    }

    private Point trilateration(Map<String, Double> distances) {
        Point p1 = BEACON_LOCATIONS.get("BEACON_1");
        Point p2 = BEACON_LOCATIONS.get("BEACON_2");
        Point p3 = BEACON_LOCATIONS.get("BEACON_3");

        double r1 = distances.get("BEACON_1");
        double r2 = distances.get("BEACON_2");
        double r3 = distances.get("BEACON_3");

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

    private void updateLocation(Point estimatedPosition) {
        runOnUiThread(() -> mapView.updateUserPosition(estimatedPosition));
    }

    private void updateInfoTextView(Point estimatedPosition) {
        StringBuilder info = new StringBuilder();
        info.append("Estimated Position: (")
                .append(estimatedPosition.x)
                .append(", ")
                .append(estimatedPosition.y)
                .append(")\n\nBeacons:\n");

        for (Map.Entry<String, Double> entry : distances.entrySet()) {
            info.append("UUID: ").append(entry.getKey())
                    .append("\nDistance: ").append(entry.getValue())
                    .append(" meters\n\n");
        }

        runOnUiThread(() -> infoTextView.setText(info.toString()));
    }

    static class Point {
        final double x, y;
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static class KalmanFilter {
        private double Q = 0.00001;
        private double R = 0.001;
        private double A = 1;
        private double B = 0;
        private double C = 1;
        private double cov = Double.NaN;
        private double x = Double.NaN;

        public double filter(double z) {
            if (Double.isNaN(x)) {
                x = 1.0 / C * z;
                cov = 1.0 / C * R * 1.0 / C;
            } else {
                double predX = A * x + B * 0;
                double predCov = A * cov * A + Q;

                double K = predCov * C * 1.0 / (C * predCov * C + R);
                x = predX + K * (z - C * predX);
                cov = predCov - K * C * predCov;
            }
            return x;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothPermissions();
            } else {
                Log.e("MainActivity", "위치 권한이 필요합니다.");
            }
        } else if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBeaconScan();
            } else {
                Log.e("MainActivity", "블루투스 권한이 필요합니다.");
            }
        }
    }
}
