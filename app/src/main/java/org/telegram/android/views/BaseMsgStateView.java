package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.ui.FontController;

import java.util.HashMap;

/**
 * Created by ex3ndr on 13.03.14.
 */
public abstract class BaseMsgStateView extends BaseMsgView {

    private static final long PENDING_QUITE_TIME = 1000;

    private static final int COLOR_CLOCK = 0xff69b449;
    private static final int COLOR_DATE_OUT = 0xff7ebe6b;
    private static final int COLOR_DATE_IN = 0xffa1aab3;
    private static final int COLOR_DATE_ERROR = 0xffDB4942;

    private static final int SIZE_ICON = 10;
    private static final int SIZE_HEIGHT = SIZE_ICON;

    private static final int SIZE_ICON_DELTA = 4;
    private static final int SIZE_FULL_PADDING = 18;

    private static final int SIZE_CLOCK = 5;
    private static final int SIZE_CLOCK_H = 4;
    private static final int SIZE_CLOCK_M = 3;

    private static HashMap<Long, Long> pendingTimes = new HashMap<Long, Long>();

    private static Paint clockPaint;
    private static TextPaint dateTextPaint;
    private static boolean isLoaded = false;

    private static synchronized void checkResources(Context context) {
        if (isLoaded) {
            return;
        }
        clockPaint = new Paint();
        clockPaint.setStyle(Paint.Style.STROKE);
        clockPaint.setColor(COLOR_CLOCK);
        clockPaint.setStrokeWidth(px(1));
        clockPaint.setAntiAlias(true);
        clockPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        dateTextPaint = FontController.createTextPaint(context, 12);
        isLoaded = true;
    }

    private int clockRadius;
    private int clockRadiusHour;
    private int clockRadiusMinutes;

    private int sizeIcon;
    private int sizeHeight;
    private int sizeDelta;

    protected boolean isOut;
    protected String date;
    protected int dateWidth;
    protected int dateHeight;
    protected int dateFullWidth;
    protected int state;
    protected int prevState;
    protected long stateChangeTime;
    protected Rect rect = new Rect();

    private Drawable halfcheck;
    private Drawable check;
    private Drawable error;

    public BaseMsgStateView(Context context) {
        super(context);
    }

    public BaseMsgStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseMsgStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init() {
        super.init();
        checkResources(getContext());

        clockRadius = getPx(SIZE_CLOCK);
        clockRadiusHour = getPx(SIZE_CLOCK_H);
        clockRadiusMinutes = getPx(SIZE_CLOCK_M);

        halfcheck = createHalfcheck();
        check = createCheck();
        error = createError();
        sizeHeight = px(SIZE_HEIGHT);
        sizeIcon = px(SIZE_ICON);
        sizeDelta = px(SIZE_ICON_DELTA);
    }

    protected Drawable createCheck() {
        return getResources().getDrawable(R.drawable.st_bubble_ic_check);
    }

    protected Drawable createHalfcheck() {
        return getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck);
    }

    protected Drawable createError() {
        return getResources().getDrawable(R.drawable.st_bubble_ic_warning);
    }


    protected void bindStateNew(MessageWireframe msg) {
        this.state = msg.message.getState();
        if (state == MessageState.PENDING) {
            if (pendingTimes.containsKey(msg.randomId)) {
                this.stateChangeTime = pendingTimes.get(msg.randomId);
            } else {
                this.stateChangeTime = SystemClock.uptimeMillis();
                pendingTimes.put(msg.randomId, this.stateChangeTime);
            }

        }
        this.prevState = -1;
        date = msg.date;
        isOut = msg.message.isOut();
        bindState(msg);
    }

    protected void bindStateUpdate(MessageWireframe msg) {
        if (this.state != msg.message.getState()) {
            this.prevState = this.state;
            this.state = msg.message.getState();
            this.stateChangeTime = SystemClock.uptimeMillis();
        }
        date = msg.date;
        bindState(msg);
    }

    private void bindState(MessageWireframe msg) {
        dateTextPaint.getTextBounds(date, 0, date.length(), rect);

        dateWidth = (int) dateTextPaint.measureText(date);
        dateHeight = -rect.top;
        if (msg.message.isOut()) {
            dateFullWidth = dateWidth + getPx(SIZE_FULL_PADDING);
        } else {
            dateFullWidth = dateWidth;
        }
    }

    protected boolean drawState(Canvas canvas, int paddingRight, int paddingBottom) {
        int right = getBubbleContentWidth() - paddingRight;
        int bottom = getBubbleContentHeight() - paddingBottom;
        int top = bottom - sizeHeight;
        int left = right - dateFullWidth;

        int baseline = bottom + (dateHeight - sizeHeight) / 2;
        int iconTop = top + (sizeIcon - sizeHeight) / 2;
        int iconLeft = right - sizeIcon;

        boolean isAnimated = false;
        int color;
        if (isOut) {
            if (state == MessageState.PENDING) {
                long animationTime = SystemClock.uptimeMillis() - stateChangeTime;
                if (animationTime > PENDING_QUITE_TIME) {
                    canvas.save();
                    canvas.translate(iconLeft - sizeDelta, iconTop);
                    canvas.drawCircle(clockRadius, clockRadius, clockRadius, clockPaint);
                    double time = (System.currentTimeMillis() / 15.0) % (12 * 60);
                    double angle = (time / (6 * 60)) * Math.PI;

                    int x = (int) (Math.sin(-angle) * clockRadiusHour);
                    int y = (int) (Math.cos(-angle) * clockRadiusHour);
                    canvas.drawLine(clockRadius, clockRadius, clockRadius + x, clockRadius + y, clockPaint);

                    x = (int) (Math.sin(-angle * 12) * clockRadiusMinutes);
                    y = (int) (Math.cos(-angle * 12) * clockRadiusMinutes);
                    canvas.drawLine(clockRadius, clockRadius, clockRadius + x, clockRadius + y, clockPaint);

                    canvas.restore();
                }

                color = COLOR_DATE_OUT;
                isAnimated = true;
            } else if (state == MessageState.READED && prevState == MessageState.SENT && (SystemClock.uptimeMillis() - stateChangeTime < STATE_ANIMATION_TIME)) {
                long animationTime = SystemClock.uptimeMillis() - stateChangeTime;
                float progress = easeStateFade(animationTime / (float) STATE_ANIMATION_TIME);
                int alphaNew = (int) (progress * 255);
                float scale = 1 + progress * 0.2f;

                bounds(check, iconLeft - sizeDelta, iconTop);
                check.setAlpha(255);
                check.draw(canvas);

                bounds(halfcheck, iconLeft, iconTop, scale);
                halfcheck.setAlpha(alphaNew);
                halfcheck.draw(canvas);

                color = COLOR_DATE_OUT;
                isAnimated = true;
            } else {
                if (state == MessageState.READED) {
                    bounds(check, iconLeft - sizeDelta, iconTop);
                    check.setAlpha(255);
                    check.draw(canvas);

                    bounds(halfcheck, iconLeft, iconTop);
                    halfcheck.setAlpha(255);
                    halfcheck.draw(canvas);
                } else {
                    Drawable stateDrawable = state == MessageState.FAILURE ? error : check;
                    bounds(stateDrawable, iconLeft - sizeDelta, iconTop);
                    stateDrawable.setAlpha(255);
                    stateDrawable.draw(canvas);
                }

                if (state == MessageState.FAILURE) {
                    color = COLOR_DATE_ERROR;
                } else {
                    color = COLOR_DATE_OUT;
                }
            }
        } else {
            color = COLOR_DATE_IN;
        }

        dateTextPaint.setColor(color);
        canvas.drawText(date, left, baseline, dateTextPaint);

        return isAnimated;
    }
}
