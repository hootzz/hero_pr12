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
    private Point userPosition = new Point(0, 0); // 초기 사용자 위치를 (0, 0)으로 설정
    private Map<String, Point> beaconPositions;
    private Paint gridPaint;
    private Paint pointPaint;
    private Paint arrowPaint;
    private Paint markerPaint;
    private static final int GRID_SIZE = 50; // 기본 격자의 크기
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

    public void updateBeaconPositions(Map<String, Point> beaconPositions) {
        this.beaconPositions = beaconPositions;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Canvas 조작
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        canvas.translate(offsetX / scaleFactor, offsetY / scaleFactor);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // 평면도 그리기
        Bitmap buildingPlanBitmap = BuildingPlanLoader.getBuildingPlanBitmap();
        if (buildingPlanBitmap != null) {
            // 평면도의 크기 가져오기
            int planWidth = buildingPlanBitmap.getWidth();
            int planHeight = buildingPlanBitmap.getHeight();

            // 평면도를 그리드의 중앙에 맞추기 위해 위치 조정
            float planLeft = centerX - (planWidth / 2) + 1070;
            float planTop = centerY - (planHeight / 2) - 220;
            canvas.drawBitmap(buildingPlanBitmap, planLeft, planTop, null);
        }

        // 그리드 그리기
        for (int i = -100 * GRID_SIZE; i <= 100 * GRID_SIZE; i += GRID_SIZE) {
            canvas.drawLine(i, -100 * GRID_SIZE, i, 100 * GRID_SIZE, gridPaint);
        }
        for (int j = -100 * GRID_SIZE; j <= 100 * GRID_SIZE; j += GRID_SIZE) {
            canvas.drawLine(-100 * GRID_SIZE, j, 100 * GRID_SIZE, j, gridPaint);
        }

        // 비콘 위치 그리기
        if (beaconPositions != null) {
            for (Map.Entry<String, Point> entry : beaconPositions.entrySet()) {
                String beaconKey = entry.getKey();
                Point point = entry.getValue();
                float beaconX = (float) (centerX + (point.x - userPosition.x) * GRID_SIZE);
                float beaconY = (float) (centerY - (point.y - userPosition.y) * GRID_SIZE); // Y축 방향을 맞추기 위해 -
                Paint beaconPaint = new Paint();
                beaconPaint.setColor(BeaconInfoLoader.beaconColors.get(beaconKey));
                beaconPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(beaconX, beaconY, 10 / scaleFactor, beaconPaint);
            }
        }

        // 사용자 위치 그리기 (중앙)
        canvas.drawCircle(centerX, centerY, 20 / scaleFactor, pointPaint);

        // 사용자 방향 삼각형 그리기
        drawUserDirectionTriangle(canvas, centerX, centerY, userOrientation);

        // 사용자 마커 그리기
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
            offsetX -= distanceX / scaleFactor;
            offsetY -= distanceY / scaleFactor;
            invalidate();
            return true;
        }
    }
}
