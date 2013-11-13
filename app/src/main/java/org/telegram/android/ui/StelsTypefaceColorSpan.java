package org.telegram.android.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Author: Korshakov Stepan
 * Created: 02.09.13 14:55
 */
public class StelsTypefaceColorSpan extends StelsTypefaceSpan {
    private int color;

    public StelsTypefaceColorSpan(int color, String kind, Context context, boolean fakeBold) {
        super(kind, context, fakeBold);
        this.color = color;
    }

    @Override
    protected void applyCustomTypeFace(Paint paint, Typeface tf) {
        super.applyCustomTypeFace(paint, tf);
        paint.setColor(color);
    }
}
