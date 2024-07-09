package com.example.hero_pr12;

import android.widget.TextView;

import java.util.Map;

public class UIUpdater {
    private TextView infoTextView;
    private MapView mapView;

    public UIUpdater(TextView infoTextView, MapView mapView) {
        this.infoTextView = infoTextView;
        this.mapView = mapView;
    }

    public void updateInfoTextView(Map<String, Double> distances) {
        StringBuilder info = new StringBuilder();
        info.append("Beacons:\n");
        for (Map.Entry<String, Double> entry : distances.entrySet()) {
            info.append("UUID: ").append(entry.getKey())
                    .append("\nDistance: ").append(entry.getValue())
                    .append(" meters\n\n");
        }
        infoTextView.setText(info.toString());
    }

    public void updateLocation(Point location) {
        mapView.updateUserPosition(location);
    }

    public void updateUserOrientation(float orientation) {
        mapView.updateUserOrientation(orientation);
    }
}