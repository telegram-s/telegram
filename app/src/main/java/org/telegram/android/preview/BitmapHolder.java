package org.telegram.android.preview;

import android.graphics.Bitmap;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class BitmapHolder {
    private Bitmap bitmap;
    private String key;
    private int realW;
    private int realH;

    public BitmapHolder(Bitmap bitmap, String key, int realW, int realH) {
        this.bitmap = bitmap;
        this.key = key;
        this.realW = realW;
        this.realH = realH;
    }

    public BitmapHolder(Bitmap bitmap, String key) {
        this.bitmap = bitmap;
        this.key = key;
        this.realW = bitmap.getWidth();
        this.realH = bitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getKey() {
        return key;
    }

    public int getRealW() {
        return realW;
    }

    public int getRealH() {
        return realH;
    }
}
