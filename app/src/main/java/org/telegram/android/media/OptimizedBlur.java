package org.telegram.android.media;

import android.graphics.Bitmap;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.BitmapUtils;

/**
 * Created by ex3ndr on 03.02.14.
 */
public class OptimizedBlur {

    private static final String TAG = "OptimizedBlur";

    public OptimizedBlur() {
        System.loadLibrary("timg");
    }

    public synchronized Bitmap performBlur(Bitmap src) {

        if (src.getWidth() <= 90 && src.getHeight() <= 90) {
            long start = System.currentTimeMillis();
            nativeFastBlur(src);
            Logger.d(TAG, "perform native blur in " + (System.currentTimeMillis() - start) + " ms");
            return src;
        }

        Bitmap res;
        long start = System.currentTimeMillis();
        res = BitmapUtils.fastblur(src, 3);
        Logger.d(TAG, "perform java blur in " + (System.currentTimeMillis() - start) + " ms");
        return res;
    }

    protected native void nativeFastBlur(Bitmap src);
}
