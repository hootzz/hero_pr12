// MapView.java
package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.Map;

public class MapView extends View {
    private Point userPosition = new Point(0, 0); // 초기 사용자 위치를 (0, 0)으로 설정
    private Map<String, Point> beaconPositions;
    private Paint gridPaint;
    private Paint pointPaint;
    private Paint beaconPaint;
    private Paint arrowPaint;
    private Paint markerPaint;
    private static final int GRID_SIZE = 50; // 격자의 크기
    private float userOrientation = 0.0f;

    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.FILL);

        beaconPaint = new Paint();
        beaconPaint.setColor(Color.BLUE);
        beaconPaint.setStyle(Paint.Style.FILL);

        arrowPaint = new Paint();
        arrowPaint.setColor(Color.GREEN);
        arrowPaint.setStyle(Paint.Style.FILL);

        markerPaint = new Paint();
        markerPaint.setColor(Color.BLUE);
        markerPaint.setStyle(Paint.Style.FILL);
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
        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;

        // 격자 그리기
        for (int i = 0; i <= width; i += GRID_SIZE) {
            canvas.drawLine(i, 0, i, height, gridPaint);
        }
        for (int j = 0; j <= height; j += GRID_SIZE) {
            canvas.drawLine(0, j, width, j, gridPaint);
        }

        // 비콘 위치 그리기
        if (beaconPositions != null) {
            for (Point point : beaconPositions.values()) {
                float beaconX = (float) (centerX + (point.x - userPosition.x) * GRID_SIZE);
                float beaconY = (float) (centerY - (point.y - userPosition.y) * GRID_SIZE); // Y축 방향을 맞추기 위해 -
                canvas.drawCircle(beaconX, beaconY, 10, beaconPaint);
            }
        }

        // 사용자 위치 그리기 (중앙)
        canvas.drawCircle(centerX, centerY, 20, pointPaint);

        // 사용자 방향 삼각형 그리기
        drawUserDirectionTriangle(canvas, centerX, centerY, userOrientation);

        // 사용자 마커 그리기
        float markerRadius = 20;
        canvas.drawCircle(centerX, centerY, markerRadius, markerPaint);
    }

    private void drawUserDirectionTriangle(Canvas canvas, int centerX, int centerY, float orientation) {
        float arrowLength = 50;
        float halfBase = 15;

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
}
