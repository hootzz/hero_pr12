package com.example.hero_pr12;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorHandler implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor stepDetector;
    private BeaconManager beaconManager;

    private float[] gravity;
    private float[] geomagnetic;
    private float azimuth = 0.0f;
    private int stepCount = 0;
    private static final float STEP_LENGTH = 0.78f; // 평균 걸음 길이(단위: 미터)

    public SensorHandler(Context context, BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            stepCount++;
            updatePositionWithStep();
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation contains: azimuth, pitch and roll
                azimuth = (azimuth + 360) % 360; // Convert azimuth to 0-360 degrees
                beaconManager.updateOrientationData(azimuth, beaconManager.getCurrentAngle(), beaconManager.getCurrentSpeed());
                Log.d("SensorHandler", "Azimuth: " + azimuth);
            }
        }
    }

    private void updatePositionWithStep() {
        // 사용자의 방향(azimuth)에 따라 위치를 업데이트합니다.
        float distance = stepCount * STEP_LENGTH;
        double deltaX = distance * Math.cos(Math.toRadians(azimuth));
        double deltaY = distance * Math.sin(Math.toRadians(azimuth));
        beaconManager.updateUserPosition(deltaX, deltaY);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
}
