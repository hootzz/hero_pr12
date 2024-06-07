// UIUpdater.java
package com.example.hero_pr12;

import android.widget.TextView;
import java.util.Map; // 추가된 임포트
import java.util.Map.Entry; // 추가된 임포트

public class UIUpdater {
    private TextView infoTextView;
    private MapView mapView;

    public UIUpdater(TextView infoTextView, MapView mapView) {
        this.infoTextView = infoTextView;
        this.mapView = mapView;
    }

    public void updateInfoTextView(Map<String, Double> distances) {
        StringBuilder sb = new StringBuilder("Beacons:\n");
        for (Entry<String, Double> entry : distances.entrySet()) { // Map.Entry를 Entry로 변경
            sb.append("UUID: ").append(entry.getKey()).append("\n")
                    .append("Distance: ").append(entry.getValue()).append(" meters\n");
        }
        infoTextView.setText(sb.toString());
    }

    public void updateLocation(Point userPosition) {
        mapView.updateUserPosition(userPosition);
    }

    public void updateOrientation(float orientation) {
        mapView.updateUserOrientation(orientation);
    }
}
