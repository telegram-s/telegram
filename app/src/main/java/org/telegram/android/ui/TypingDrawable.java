package org.telegram.android.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

/**
 * Created by ex3ndr on 25.03.14.
 */
public class TypingDrawable extends Drawable {

    private static final int DURATION = 600;
    private static final int ANIMATION_DELTA = 800;
    private static final int ANIMATION_OFFSET = DURATION / 3;

    private static final int OFFSET = 0;
    private static final int RADIUS = 4;
    private static final int DIAMETER = RADIUS * 2;
    private static final int HEIGHT = 16;
    private static final int PADDING = 4;
    private static final int TOP = 5;

    private Paint paint;

    public TypingDrawable() {
        paint = new Paint();
        paint.setColor(0xffdae1ea);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) (UiMeasure.DENSITY * ((DIAMETER + OFFSET) * 3 + PADDING));
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) (UiMeasure.DENSITY * HEIGHT);
    }

    private float process(int offset) {
        long time = (System.currentTimeMillis() + offset) % (DURATION + ANIMATION_DELTA);
        if (time > DURATION) {
            return 0;
        }
        float p = time / (float) (DURATION);
        return (float) Math.abs(Math.sin(Math.PI * p));
    }

    @Override
    public void draw(Canvas canvas) {
        float size1 = 0.5f + (0.5f * process(ANIMATION_OFFSET * 2));
        float size2 = 0.5f + (0.5f * process(ANIMATION_OFFSET));
        float size3 = 0.5f + (0.5f * process(0));
        canvas.drawCircle(UiMeasure.DENSITY * RADIUS, UiMeasure.DENSITY * (TOP + RADIUS), size1 * UiMeasure.DENSITY * RADIUS, paint);
        canvas.drawCircle(UiMeasure.DENSITY * (RADIUS + DIAMETER + OFFSET), UiMeasure.DENSITY * (TOP + RADIUS), size2 * UiMeasure.DENSITY * RADIUS, paint);
        canvas.drawCircle(UiMeasure.DENSITY * (RADIUS + (DIAMETER + OFFSET) * 2), UiMeasure.DENSITY * (TOP + RADIUS), size3 * UiMeasure.DENSITY * RADIUS, paint);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }
}
