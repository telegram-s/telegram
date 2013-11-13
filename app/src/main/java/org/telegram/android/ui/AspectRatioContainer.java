package org.telegram.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.09.13
 * Time: 13:57
 */
public class AspectRatioContainer extends ViewGroup {
    private float aspectRatio;

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        requestLayout();
    }

    public AspectRatioContainer(Context context) {
        super(context);
    }

    public AspectRatioContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (aspectRatio != 0) {
            int w = r - l;
            int h = b - t;
            if (w * aspectRatio < h) {
                w = (int) (h / aspectRatio);
            } else {
                h = (int) (w * aspectRatio);
            }
            int xOffset = (r - l - w) / 2;
            int yOffset = (b - t - h) / 2;
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(xOffset, yOffset, xOffset + w, yOffset + h);
            }
        } else {
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(0, 0, r - l, b - t);
            }
        }
    }
}
