package org.telegram.android.ui;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import org.telegram.android.log.Logger;

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

        if (dest != null && dest.width() == src.width()) {
            if (lastTopHeight >= src.height()) {
                destH = lastTopHeight;
            } else {
                lastTopHeight = destH;
            }
        } else {
            lastTopHeight = destH;
        }

        float scaleFactor = Math.max(
                destW / (float) bitmap.getWidth(),
                destH / (float) bitmap.getHeight());
        int width = (int) (scaleFactor * bitmap.getWidth());
        int height = (int) (scaleFactor * bitmap.getHeight());
        int deltaX = -(destW - width) / 2;
        int deltaY = -(destH - height) / 2;
        deltaX /= scaleFactor;
        deltaY /= scaleFactor;
        dest = new Rect(src.left, src.top, src.right, src.top + destH);
        source = new Rect(deltaX, deltaY, (int) (destW / scaleFactor) + deltaX, (int) (destH / scaleFactor) + deltaY);
        Logger.d("FastBackgroundDrawable", "State: " + deltaX + ", " + deltaY + " - " + destW + ", " + destH);
        Logger.d("FastBackgroundDrawable", "State2: " + bitmap.getWidth() + ", " + bitmap.getHeight() + ", " + scaleFactor);
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
