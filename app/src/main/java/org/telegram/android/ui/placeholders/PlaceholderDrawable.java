package org.telegram.android.ui.placeholders;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import org.telegram.android.ui.Placeholders;

/**
 * Created by ex3ndr on 03.03.14.
 */
public class PlaceholderDrawable extends Drawable {

    private Paint rectPaint;
    private Drawable base;

    public PlaceholderDrawable(int index, int resource, Context context) {
        this.rectPaint = new Paint();
        this.rectPaint.setColor(Placeholders.getBgColor(index));
        this.base = context.getResources().getDrawable(resource);
    }

    public PlaceholderDrawable(int resource, Context context) {
        this.rectPaint = new Paint();
        this.rectPaint.setColor(Placeholders.GREY);
        this.base = context.getResources().getDrawable(resource);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(getBounds(), rectPaint);
        base.setBounds(getBounds());
        base.draw(canvas);
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
