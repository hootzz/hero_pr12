package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Map;

public class MapView extends View {
    private Point userPosition = new Point(0, 0);
    private Map<String, Point> beaconPositions;
    private Paint gridPaint;
    private Paint pointPaint;
    private Paint arrowPaint;
    private Paint markerPaint;
    private static final int GRID_SIZE = 50;
    private float userOrientation = 0.0f;

    private float scaleFactor = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public MapView(Context context) {
        super(context);
        init(context);
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.FILL);

        arrowPaint = new Paint();
        arrowPaint.setColor(Color.RED);
        arrowPaint.setStyle(Paint.Style.FILL);

        markerPaint = new Paint();
        markerPaint.setColor(Color.RED);
        markerPaint.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void updateUserPosition(Point userPosition) {
        this.userPosition = userPosition;
        invalidate();
    }

    public void updateUserOrientation(float orientation) {
        this.userOrientation = orientation;
        invalidate();
    }

    public void setBeaconPositions(Map<String, Point> beaconPositions) {
        this.beaconPositions = beaconPositions;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        canvas.translate(offsetX / scaleFactor, offsetY / scaleFactor);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // Draw building plan
        Bitmap buildingPlanBitmap = BuildingPlanLoader.getBuildingPlanBitmap();
        if (buildingPlanBitmap != null) {
            int planWidth = buildingPlanBitmap.getWidth();
            int planHeight = buildingPlanBitmap.getHeight();
            float planLeft = centerX - planWidth / 2f - (float)userPosition.x;
            float planTop = centerY - planHeight / 2f - (float)userPosition.y;
            canvas.drawBitmap(buildingPlanBitmap, planLeft, planTop, null);
        }

        // Draw grid
        for (int i = -100 * GRID_SIZE; i <= 100 * GRID_SIZE; i += GRID_SIZE) {
            float x = centerX + i - (float)userPosition.x;
            canvas.drawLine(x, -100 * GRID_SIZE, x, 100 * GRID_SIZE, gridPaint);
        }
        for (int j = -100 * GRID_SIZE; j <= 100 * GRID_SIZE; j += GRID_SIZE) {
            float y = centerY + j - (float)userPosition.y;
            canvas.drawLine(-100 * GRID_SIZE, y, 100 * GRID_SIZE, y, gridPaint);
        }

        // Draw beacons
        if (beaconPositions != null) {
            for (Map.Entry<String, Point> entry : beaconPositions.entrySet()) {
                String beaconKey = entry.getKey();
                Point point = entry.getValue();
                float beaconX = centerX + (float)(point.x - userPosition.x) * GRID_SIZE;
                float beaconY = centerY + (float)(point.y - userPosition.y) * GRID_SIZE;
                Paint beaconPaint = new Paint();
                beaconPaint.setColor(BeaconInfoLoader.beaconColors.get(beaconKey));
                beaconPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(beaconX, beaconY, 10 / scaleFactor, beaconPaint);
            }
        }

        // Draw user position (always at center)
        canvas.drawCircle(centerX, centerY, 20 / scaleFactor, pointPaint);

        // Draw user direction triangle
        drawUserDirectionTriangle(canvas, centerX, centerY, userOrientation);

        // Draw user marker
        float markerRadius = 20 / scaleFactor;
        canvas.drawCircle(centerX, centerY, markerRadius, markerPaint);

        canvas.restore();
    }

    private void drawUserDirectionTriangle(Canvas canvas, int centerX, int centerY, float orientation) {
        float arrowLength = 50 / scaleFactor;
        float halfBase = 15 / scaleFactor;

        float rad = (float) Math.toRadians(orientation);

        float tipX = (float) (centerX + arrowLength * Math.cos(rad));
        float tipY = (float) (centerY + arrowLength * Math.sin(rad));

        float leftX = (float) (centerX + halfBase * Math.cos(rad + Math.PI / 2));
        float leftY = (float) (centerY + halfBase * Math.sin(rad + Math.PI / 2));

        float rightX = (float) (centerX + halfBase * Math.cos(rad - Math.PI / 2));
        float rightY = (float) (centerY + halfBase * Math.sin(rad - Math.PI / 2));

        Path path = new Path();
        path.moveTo(tipX, tipY);
        path.lineTo(leftX, leftY);
        path.lineTo(rightX, rightY);
        path.close();

        canvas.drawPath(path, arrowPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            offsetX -= distanceX;
            offsetY -= distanceY;
            invalidate();
            return true;
        }
    }
}