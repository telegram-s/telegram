package org.telegram.android.util;

import android.graphics.Bitmap;
import org.telegram.android.ui.BitmapUtils;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class ImageNativeUtils {

    static {
        NativeLibLoader.loadLib();
    }

    public static Bitmap[] loadEmoji(String color, String alpha) {
        return nativeLoadEmoji(color, alpha);
    }

    public static void mergeBitmapAlpha(Bitmap source, Bitmap alpha) {
        nativeMergeBitmapAlpha(source, alpha);
    }

    public static Bitmap performBlur(Bitmap src) {
        if (src.getWidth() <= 90 && src.getHeight() <= 90) {
            nativeFastBlur(src, src.getWidth(), src.getHeight());
            return src;
        }

        return BitmapUtils.fastblur(src, 7);
    }

    public static Bitmap performBlur(Bitmap src, int w, int h) {
        if (src.getWidth() <= 90 && src.getHeight() <= 90) {
            nativeFastBlur(src, w, h);
            return src;
        }

        return BitmapUtils.fastblur(src, w, h, 7);
    }

    private static native boolean nativeFastBlur(Bitmap src, int w, int h);

    private static native boolean nativeMergeBitmapAlpha(Bitmap source, Bitmap alpha);

    private static native Bitmap[] nativeLoadEmoji(String colorPath, String alphaPath);
}
