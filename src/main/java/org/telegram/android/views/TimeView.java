package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.ui.FontController;

/**
 * Author: Korshakov Stepan
 * Created: 11.08.13 21:33
 */
public class TimeView extends BaseView {

    private static final long ANIMATION_DURATION = 300;

    public TimeView(Context context) {
        super(context);
        init();
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTypeface(FontController.loadTypeface(getContext(), "italic"));
        timePaint.setTextSize(getSp(12.5f));
        timePaint.setColor(0xff70B15C);

        statePending = getResources().getDrawable(R.drawable.st_bubble_ic_clock);
        stateSent = getResources().getDrawable(R.drawable.st_bubble_ic_check);
        stateHalfCheck = getResources().getDrawable(R.drawable.st_bubble_ic_halfcheck);
        stateFailure = getResources().getDrawable(R.drawable.st_bubble_ic_warning);
    }

    private Drawable statePending;
    private Drawable stateSent;
    private Drawable stateHalfCheck;
    private Drawable stateFailure;

    private int normalColor = 0xff70B15C;
    private int errorColor = 0xffDB4942;
    private TextPaint timePaint;
    private String time;
    private int offset;

    private int state = -1;
    private int oldState = -1;
    private long animationStart = 0;
    private boolean pendingAnimation;
    private boolean animated = false;
    private int oldMsgId = -1;
    private boolean animationsEnabled = true;

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    public void setState(int msgId, String time, int state) {
        this.oldState = this.state;
        this.time = time;
        this.state = state;

        if (oldMsgId == msgId) {
            if (this.oldState != this.state && this.oldState != -1) {
                pendingAnimation = true;
            } else {
                pendingAnimation = false;
                animated = false;
            }
        }
        oldMsgId = msgId;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        offset = (int) timePaint.measureText(time);
        setMeasuredDimension((int) (offset + getPx(23)), getPx(15));
    }

    private Drawable getStateDrawable(int state) {
        switch (state) {
            default:
            case MessageState.SENT:
                return stateSent;
            case MessageState.READED:
                return stateHalfCheck;
            case MessageState.FAILURE:
                return stateFailure;
            case MessageState.PENDING:
                return statePending;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (time == null)
            return;

        if (pendingAnimation) {
            animationStart = SystemClock.uptimeMillis();
            animated = true;
            pendingAnimation = false;
        }

        long animationTime = SystemClock.uptimeMillis() - animationStart;
        if (animationTime < ANIMATION_DURATION && animated && animationsEnabled) {
            if (oldState == MessageState.SENT && state == MessageState.READED) {
                int offset = (int) ((getPx(5) * animationTime) / (float) ANIMATION_DURATION);
                int alphaNew = (int) ((animationTime / (float) ANIMATION_DURATION) * 255);

                bounds(stateSent, getWidth() - stateSent.getIntrinsicWidth() - offset,
                        0);
                stateSent.setAlpha(255);
                stateSent.draw(canvas);

                bounds(stateHalfCheck, getWidth() - stateSent.getIntrinsicWidth() + getPx(5) - offset,
                        0);
                stateHalfCheck.setAlpha(alphaNew);
                stateHalfCheck.draw(canvas);

                timePaint.setColor(normalColor);
            } else {

                int alphaSrc = (int) ((1 - animationTime / (float) ANIMATION_DURATION) * 255);
                int alphaNew = (int) ((animationTime / (float) ANIMATION_DURATION) * 255);

                Drawable oldStateDrawable = getStateDrawable(oldState);
                Drawable stateDrawable = getStateDrawable(state);

                bounds(oldStateDrawable, getWidth() - oldStateDrawable.getIntrinsicWidth(),
                        0);
                oldStateDrawable.setAlpha(alphaSrc);
                oldStateDrawable.draw(canvas);

                if (state == MessageState.READED) {
                    bounds(stateSent, getWidth() - stateSent.getIntrinsicWidth() - getPx(5),
                            0);
                    stateSent.setAlpha(alphaNew);
                    stateSent.draw(canvas);
                }

                bounds(stateDrawable, getWidth() - stateDrawable.getIntrinsicWidth(),
                        0);
                stateDrawable.setAlpha(alphaNew);
                stateDrawable.draw(canvas);


                if (state == MessageState.FAILURE) {
                    timePaint.setColor(errorColor);
                } else {
                    timePaint.setColor(normalColor);
                }
            }

            invalidate();
        } else {
            Drawable stateDrawable = getStateDrawable(state);
            bounds(stateDrawable, getWidth() - stateDrawable.getIntrinsicWidth(),
                    0);
            stateDrawable.setAlpha(255);
            stateDrawable.draw(canvas);

            if (state == MessageState.READED) {
                bounds(stateSent, getWidth() - stateSent.getIntrinsicWidth() - getPx(5),
                        0);
                stateSent.setAlpha(255);
                stateSent.draw(canvas);
            }

            if (state == MessageState.FAILURE) {
                timePaint.setColor(errorColor);
            } else {
                timePaint.setColor(normalColor);
            }

            animated = false;
        }

        canvas.drawText(time, 0, getPx(11), timePaint);
    }
}