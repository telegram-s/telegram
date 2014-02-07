package org.telegram.android.media;

import android.graphics.Bitmap;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.BitmapUtils;

/**
 * Created by ex3ndr on 03.02.14.
 */
public class OptimizedBlur {

    static {
        System.loadLibrary("timg");
    }

    public synchronized Bitmap performBlur(Bitmap src) {
        if (src.getWidth() <= 90 && src.getHeight() <= 90) {
            nativeFastBlur(src);
            return src;
        }

        return BitmapUtils.fastblur(src, 3);
    }

    protected native void nativeFastBlur(Bitmap src);
}
