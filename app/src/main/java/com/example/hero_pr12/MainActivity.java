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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                if (beaconId != null && BEACON_LOCATIONS.containsKey(beaconId)) {
                    double filteredRssi = calculateFilteredRssi(beaconId, rssi);
                    double distance = calculateDistance(filteredRssi);
                    double filteredDistance = applyKalmanFilter(beaconId, distance);
                    distances.put(beaconId, filteredDistance);
                    if (distances.size() >= 3) {
                        Point estimatedPosition = trilateration(distances);
                        Log.d("MainActivity", "Estimated Position: " + estimatedPosition.x + ", " + estimatedPosition.y);
                        updateLocation(estimatedPosition);
                        updateInfoTextView(estimatedPosition);
                    }
                }
            }
        };

        // 초기 비콘 위치를 설정
        mapView.updateBeaconPositions(BEACON_LOCATIONS);
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
        Log.d("MainActivity", "Starting Beacon Scan");
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    private String getBeaconId(byte[] scanRecord) {
        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // identifies an iBeacon
                    ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { // identifies correct data length
                patternFound = true;
                break;
            }
            startByte++;
        }

        if (patternFound) {
            // Convert the scan record to a UUID
            byte[] uuidBytes = new byte[16];
            System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
            ByteBuffer bb = ByteBuffer.wrap(uuidBytes);
            long high = bb.getLong();
            long low = bb.getLong();
            UUID uuid = new UUID(high, low);

            // Major
            int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

            // Minor
            int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);

            return uuid.toString() + "_" + major + "_" + minor;
        }

        return null;
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
