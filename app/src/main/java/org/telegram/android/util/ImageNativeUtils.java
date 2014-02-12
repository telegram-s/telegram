package org.telegram.android.util;

import android.graphics.Bitmap;

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

    private static native void nativeMergeBitmapAlpha(Bitmap source, Bitmap alpha);
}
