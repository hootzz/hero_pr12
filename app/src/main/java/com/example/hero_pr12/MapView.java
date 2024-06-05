package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Map;

public class MapView extends View {
    private Point userPosition;
    private Map<String, Point> beaconPositions;
    private Paint gridPaint;
    private Paint pointPaint;
    private Paint beaconPaint;
    private static final int GRID_SIZE = 50; // 격자의 크기

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
    }

    public void updateUserPosition(Point userPosition) {
        this.userPosition = userPosition;
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

        // Draw grid
        for (int i = 0; i <= width; i += GRID_SIZE) {
            canvas.drawLine(i, 0, i, height, gridPaint);
        }
        for (int j = 0; j <= height; j += GRID_SIZE) {
            canvas.drawLine(0, j, width, j, gridPaint);
        }

        // Draw beacon positions
        if (beaconPositions != null) {
            for (Point point : beaconPositions.values()) {
                canvas.drawCircle((float) point.x * GRID_SIZE, (float) point.y * GRID_SIZE, 10, beaconPaint);
            }
        }

        // Draw user position
        if (userPosition != null) {
            canvas.drawCircle((float) userPosition.x * GRID_SIZE, (float) userPosition.y * GRID_SIZE, 10, pointPaint);
        }
    }
}
