package org.telegram.android.util;

import android.graphics.Bitmap;
import org.telegram.android.ui.BitmapUtils;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class ImageNativeUtils {

    static {
        System.loadLibrary("timg");
    }

    public static void mergeBitmapAlpha(Bitmap source, Bitmap alpha) {
        nativeMergeBitmapAlpha(source, alpha);
    }

    public static Bitmap performBlur(Bitmap src) {
        if (src.getWidth() <= 90 && src.getHeight() <= 90) {
            nativeFastBlur(src);
            return src;
        }

        return BitmapUtils.fastblur(src, 3);
    }

    private static native boolean nativeFastBlur(Bitmap src);

    private static native boolean nativeMergeBitmapAlpha(Bitmap source, Bitmap alpha);
}
