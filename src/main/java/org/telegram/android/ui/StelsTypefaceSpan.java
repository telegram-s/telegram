package org.telegram.android.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * Author: Korshakov Stepan
 * Created: 21.08.13 3:22
 */
public class StelsTypefaceSpan extends TypefaceSpan {
    private final Typeface newType;
    private boolean fakeBold;

    public StelsTypefaceSpan(String kind, Context context, boolean fakeBold) {
        super("Roboto");
        this.newType = FontController.loadTypeface(context, kind);
        this.fakeBold = fakeBold;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        applyCustomTypeFace(ds, newType);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        applyCustomTypeFace(paint, newType);
    }

    protected void applyCustomTypeFace(Paint paint, Typeface tf) {
        paint.setFakeBoldText(fakeBold);
        paint.setTypeface(tf);
    }
}
