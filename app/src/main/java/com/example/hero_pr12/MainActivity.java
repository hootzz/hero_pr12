package com.example.hero_pr12;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {
    private MapView mapView;
    private TextView infoTextView;
    private BeaconManager beaconManager;
    private PermissionHandler permissionHandler;
    private UIUpdater uiUpdater;
    private SensorHandler sensorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        infoTextView = findViewById(R.id.infoTextView);
        Log.d("MainActivity", "onCreate called");

        BeaconInfoLoader.loadBeaconInfo(this);
        uiUpdater = new UIUpdater(infoTextView, mapView);
        beaconManager = new BeaconManager(this, uiUpdater);
        permissionHandler = new PermissionHandler(this, beaconManager);

        sensorHandler = new SensorHandler(this, uiUpdater);

        permissionHandler.checkAndRequestPermissions();
        mapView.updateBeaconPositions(BeaconInfoLoader.BEACON_LOCATIONS);

        sensorHandler.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode, grantResults);
    }
}
