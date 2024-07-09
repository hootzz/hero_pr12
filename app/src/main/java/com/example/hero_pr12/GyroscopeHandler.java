package com.example.hero_pr12;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GyroscopeHandler implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private BeaconManager beaconManager;
    private float currentAngle = 0.0f;

    public GyroscopeHandler(Context context, BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void start() {
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float deltaRotation = event.values[2] * event.timestamp * 0.000000001f; // Z 축 회전
            currentAngle += deltaRotation;
            beaconManager.updateOrientationData(beaconManager.getCurrentAzimuth(), currentAngle, beaconManager.getCurrentSpeed());
            Log.d("GyroscopeHandler", "Current angle: " + currentAngle);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
}