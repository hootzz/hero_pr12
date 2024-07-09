package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BuildingPlanLoader {
    private static Bitmap buildingPlanBitmap;

    public static void loadBuildingPlan(Context context, int resourceId) {
        buildingPlanBitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
    }

    public static Bitmap getBuildingPlanBitmap() {
        return buildingPlanBitmap;
    }
}
