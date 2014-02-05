package org.telegram.android.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import org.telegram.android.TelegramApplication;
import org.telegram.android.ui.UiMeasure;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 18:37
 */
public class BaseView extends View {
    protected final float density;
    protected final float dDensity;
    protected final DisplayMetrics metrics;
    protected final TelegramApplication application;

    public BaseView(Context context) {
        super(context);
        application = (TelegramApplication) context.getApplicationContext();
        metrics = context.getResources().getDisplayMetrics();
        density = metrics.density;
        dDensity = metrics.scaledDensity;
    }

    public BaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        application = (TelegramApplication) context.getApplicationContext();
        metrics = context.getResources().getDisplayMetrics();
        density = metrics.density;
        dDensity = metrics.scaledDensity;
    }

    public BaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        application = (TelegramApplication) context.getApplicationContext();
        metrics = context.getResources().getDisplayMetrics();
        density = metrics.density;
        dDensity = metrics.scaledDensity;
    }

    protected int getPx(float dp) {
        /*return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);*/
        return (int) (dp * density + 0.5f);
    }

    protected int getSp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }

    protected void bounds(Drawable d, int x, int y) {
        bounds(d, x, y, d.getIntrinsicWidth(), d.getIntrinsicHeight());
    }

    protected void bounds(Drawable d, int x, int y, int w, int h) {
        d.setBounds(x, y, x + w, y + h);
    }

    protected static int px(float dp) {
        return (int) (dp * UiMeasure.DENSITY + 0.5f);
    }

    protected static int sp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, UiMeasure.METRICS);
    }

    protected void inavalidateForAnimation() {
        if (Build.VERSION.SDK_INT >= 16) {
            postInvalidateOnAnimation();
        } else {
            postInvalidateDelayed(16);
        }
    }
}
