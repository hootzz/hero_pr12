package com.example.hero_pr12;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GyroscopeHandler implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private float[] gyroValues;
    private long lastUpdateTime;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float[] deltaRotationVector;
    private BeaconManager beaconManager;

    public GyroscopeHandler(Context context, BeaconManager beaconManager) {
        this.beaconManager = beaconManager;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gyroValues = new float[3];
        deltaRotationVector = new float[4];
    }

    public void start() {
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (lastUpdateTime != 0) {
                final float dT = (event.timestamp - lastUpdateTime) * NS2S;

                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                if (omegaMagnitude > 1e-6) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the time step
                // in order to get a delta rotation from this sample over the time step
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
            }
            lastUpdateTime = event.timestamp;
            System.arraycopy(event.values, 0, gyroValues, 0, 3);

            // Calculate orientation change
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, deltaRotationVector);
            float[] orientationValues = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationValues);

            // Convert radians to degrees
            float azimuth = (float) Math.toDegrees(orientationValues[0]);
            float pitch = (float) Math.toDegrees(orientationValues[1]);
            float roll = (float) Math.toDegrees(orientationValues[2]);

            // Update orientation in BeaconManager
            beaconManager.updateOrientationData(azimuth, pitch, roll);

            Log.d("GyroscopeHandler", "Orientation: Azimuth=" + azimuth + ", Pitch=" + pitch + ", Roll=" + roll);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    public float[] getGyroValues() {
        return gyroValues;
    }

    public float[] getDeltaRotationVector() {
        return deltaRotationVector;
    }
}