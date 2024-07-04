package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BeaconInfoLoader {
    public static final Map<String, Point> BEACON_LOCATIONS = new HashMap<>();
    public static final Map<String, Integer> beaconTxPower = new HashMap<>();
    public static final Map<String, String> beaconMacAddress = new HashMap<>();
    public static final Map<String, Integer> beaconColors = new HashMap<>();

    public static void loadBeaconInfo(Context context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("beacon_info.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String macAddress = parts[0];
                String uuid = parts[1];
                int major = Integer.parseInt(parts[2]);
                int minor = Integer.parseInt(parts[3]);
                int txPower = Integer.parseInt(parts[4]);
                double x = Double.parseDouble(parts[5]);
                double y = Double.parseDouble(parts[6]);
                int color = Color.parseColor(parts[7]);
                String beaconKey = uuid + "_" + major + "_" + minor;
                BEACON_LOCATIONS.put(beaconKey, new Point(x, y));
                beaconTxPower.put(beaconKey, txPower);
                beaconMacAddress.put(macAddress, beaconKey);
                beaconColors.put(beaconKey, color);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
