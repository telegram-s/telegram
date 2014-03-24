package org.telegram.android.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 18:51
 */
public class FastBackgroundDrawable extends Drawable {

    private Paint backgroundPaint;

    private Bitmap bitmap;
    private Rect source;
    private Rect dest;
    private int lastTop;
    private int lastTopHeight;

    public FastBackgroundDrawable(Bitmap bitmap) {
        this.backgroundPaint = new Paint();
        this.backgroundPaint.setAntiAlias(true);
        this.bitmap = bitmap;
        this.source = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        updateBounds();
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        updateBounds();
    }

    private void updateBounds() {
        Rect src = getBounds();
        int destW = src.width();
        int destH = src.height();

        float scaleFactor = Math.max(
                destW / (float) bitmap.getWidth(),
                destH / (float) bitmap.getHeight());
        int width = (int) (scaleFactor * bitmap.getWidth());
        int height = (int) (scaleFactor * bitmap.getHeight());
        int deltaX = -(destW - width) / 2;
        int deltaY = -(destH - height) / 2;
        if (dest != null && dest.width() == src.width()) {
            if (lastTopHeight >= src.height()) {
                deltaY = lastTop;
            } else {
                lastTop = deltaY;
                lastTopHeight = destH;
            }
        } else {
            lastTop = deltaY;
            lastTopHeight = destH;
        }
        deltaX /= scaleFactor;
        deltaY /= scaleFactor;
        dest = getBounds();
        source = new Rect(deltaX, deltaY, destW + deltaX, destH + deltaY);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, source, dest, backgroundPaint);
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
