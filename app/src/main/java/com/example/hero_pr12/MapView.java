package com.example.hero_pr12;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class MapView extends View {
    private MainActivity.Point userPosition;
    private Paint paint;

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
        paint = new Paint();
        paint.setColor(Color.RED);
    }

    public void updateUserPosition(MainActivity.Point userPosition) {
        this.userPosition = userPosition;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (userPosition != null) {
            // 사용자 위치 시각화 (예: 원으로 표시)
            canvas.drawCircle((float) userPosition.x, (float) userPosition.y, 10, paint);
        }
    }
}
