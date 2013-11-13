package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import org.telegram.android.ui.FontController;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 22.09.13
 * Time: 1:55
 */
public class VideoTimerView extends BaseView {

    public static final int ORIENTATION_0 = 0;
    public static final int ORIENTATION_90 = 1;
    public static final int ORIENTATION_180 = 2;
    public static final int ORIENTATION_270 = 3;

    private int orientation = ORIENTATION_0;
    private String time = "#####11";
    private TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

    public VideoTimerView(Context context) {
        super(context);
        init();
    }

    public VideoTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VideoTimerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setTypeface(FontController.loadTypeface(getContext(), "regular"));
        paint.setTextSize(getSp(18));
        paint.setColor(Color.WHITE);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
        postInvalidate();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (orientation) {
            default:
            case ORIENTATION_0:
                canvas.translate(getPx(16), getPx(24));
                break;
            case ORIENTATION_90:
                canvas.translate(getWidth() - getPx(24), getPx(16));
                canvas.rotate(90);
                break;
            case ORIENTATION_180:
                canvas.translate(getWidth() - getPx(16), getHeight() - getPx(24));
                canvas.rotate(180);
                break;
            case ORIENTATION_270:
                canvas.translate(getPx(24), getHeight() - getPx(16));
                canvas.rotate(270);
                break;
        }
        canvas.drawText(time, 0, 0, paint);
    }
}
