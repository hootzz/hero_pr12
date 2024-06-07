package com.example.hero_pr12;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;  // Log 클래스 임포트
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHandler {
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 2;
    private final Activity activity;
    private final BeaconManager beaconManager;

    public PermissionHandler(Activity activity, BeaconManager beaconManager) {
        this.activity = activity;
        this.beaconManager = beaconManager;
    }

    public void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            checkBluetoothPermissions();
        }
    }

    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_BLUETOOTH);
        } else {
            beaconManager.startBeaconScan();
        }
    }

    public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothPermissions();
            } else {
                Log.e("PermissionHandler", "위치 권한이 필요합니다.");
            }
        } else if (requestCode == PERMISSION_REQUEST_BLUETOOTH) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                beaconManager.startBeaconScan();
            } else {
                Log.e("PermissionHandler", "블루투스 권한이 필요합니다.");
            }
        }
    }
}