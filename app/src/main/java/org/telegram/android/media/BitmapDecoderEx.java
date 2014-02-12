package org.telegram.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.extradea.framework.images.BitmapDecoder;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 07.02.14.
 */
public class BitmapDecoderEx {

    private static final String TAG = "BitmapDecoderEx";

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
        long start = System.currentTimeMillis();
        new BitmapDecoderEx().nativeDecodeBitmapScaled(fileName, dest);
        Logger.d(TAG, "Decoded file in " + (System.currentTimeMillis() - start) + " ms");
    }

    public static void decodeReuseBitmap(byte[] src, Bitmap dest) {
        long start = System.currentTimeMillis();
        new BitmapDecoderEx().nativeDecodeArray(src, dest);
        Logger.d(TAG, "Decoded memory in " + (System.currentTimeMillis() - start) + " ms");
    }

    private native void nativeDecodeBitmapScaled(String fileName, Bitmap bitmap);

    private native void nativeDecodeBitmap(String fileName, Bitmap bitmap);

    private native void nativeDecodeArray(byte[] array, Bitmap bitmap);

    private native void nativeDecodeBitmapBlend(String fileName, Bitmap bitmap);
}
