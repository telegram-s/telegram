package org.telegram.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.extradea.framework.images.BitmapDecoder;

/**
 * Created by ex3ndr on 07.02.14.
 */
public class BitmapDecoderEx {

    static {
        System.loadLibrary("timg");
    }

    private BitmapDecoderEx() {
    }

    public static void decodeReuseBitmapBlend(String fileName, Bitmap dest) {
        new BitmapDecoderEx().nativeDecodeBitmapBlend(fileName, dest);
    }

    public static void decodeReuseBitmap(String fileName, Bitmap dest) {
        new BitmapDecoderEx().nativeDecodeBitmap(fileName, dest);
    }

    public static void decodeReuseBitmapScaled(String fileName, Bitmap dest) {
        new BitmapDecoderEx().nativeDecodeBitmapScaled(fileName, dest);
    }

    public static void decodeReuseBitmap(byte[] src, Bitmap dest) {
        new BitmapDecoderEx().nativeDecodeArray(src, dest);
    }

    private native void nativeDecodeBitmapScaled(String fileName, Bitmap bitmap);

    private native void nativeDecodeBitmap(String fileName, Bitmap bitmap);

    private native void nativeDecodeArray(byte[] array, Bitmap bitmap);

    private native void nativeDecodeBitmapBlend(String fileName, Bitmap bitmap);
}
