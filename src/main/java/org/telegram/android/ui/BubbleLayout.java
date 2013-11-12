package org.telegram.android.ui;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 8:04
 */
public class BubbleLayout extends ViewGroup {
    private TextView mainView;
    private View statView;

    private boolean useNewLine;

    private final DisplayMetrics metrics;

    public BubbleLayout(Context context) {
        super(context);
        metrics = context.getResources().getDisplayMetrics();
    }

    public BubbleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        metrics = context.getResources().getDisplayMetrics();
    }

    public BubbleLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        metrics = context.getResources().getDisplayMetrics();
    }

    protected int getPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    protected int getSp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }

    private void ensureViews() {
        if (mainView == null) {
            mainView = (TextView) getChildAt(0);
            statView = getChildAt(1);
        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ensureViews();
        mainView.layout(0, 0, mainView.getMeasuredWidth(), mainView.getMeasuredHeight());
        statView.layout(getMeasuredWidth() - statView.getMeasuredWidth(),
                getMeasuredHeight() - statView.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ensureViews();
        mainView.measure(widthMeasureSpec, heightMeasureSpec);
        statView.measure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        Layout layout = mainView.getLayout();
        if (layout.getLineCount() == 1) {
            if (width - layout.getWidth() < statView.getMeasuredWidth() + getPx(11)) {
                setMeasuredDimension(mainView.getMeasuredWidth(), mainView.getMeasuredHeight() + statView.getMeasuredHeight());
            } else {
                setMeasuredDimension(mainView.getMeasuredWidth() + statView.getMeasuredWidth() + getPx(11), mainView.getMeasuredHeight() + getPx(3));
            }
        } else {
            int lastLineW = (int) layout.getLineWidth(layout.getLineCount() - 1);
            if (width - lastLineW < statView.getMeasuredWidth() + getPx(11)) {
                setMeasuredDimension(mainView.getMeasuredWidth(), mainView.getMeasuredHeight() + statView.getMeasuredHeight());
            } else {
                setMeasuredDimension(
                        Math.max(mainView.getMeasuredWidth(), lastLineW + statView.getMeasuredWidth() + getPx(11)),
                        mainView.getMeasuredHeight() + getPx(3));
            }
        }
    }
}
