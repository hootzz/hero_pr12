//비콘 정보(위치,전송전력(TxPower), MAC주소 등) 로드&저장
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
            //애플리케이션의 자원에서 beacon_info.txt 파일을 읽어옴
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("beacon_info.txt")));
            String line;
            // 파일의 각 줄을 읽어서 처리
            while ((line = reader.readLine()) != null) {
                // 쉼표로 구분된 각 부분을 배열로 분리
                String[] parts = line.split(",");
                String macAddress = parts[0];
                String uuid = parts[1];
                int major = Integer.parseInt(parts[2]);
                int minor = Integer.parseInt(parts[3]);
                int txPower = Integer.parseInt(parts[4]);
                double x = Double.parseDouble(parts[5]);
                double y = Double.parseDouble(parts[6]);
                int color = Color.parseColor(parts[7]);
                //비콘의 고유키 생성
                String beaconKey = uuid + "_" + major + "_" + minor;
                //비콘의 위치정보, 전송전력, MAC주소, 색상정보 저장
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
