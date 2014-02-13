package org.telegram.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.extradea.framework.images.BitmapDecoder;
import org.telegram.android.log.Logger;

import java.io.IOException;

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

    public static void decodeReuseBitmapBlend(String fileName, Bitmap dest) throws IOException {
        nativeDecodeBitmapBlend(fileName, dest);
    }

    public static void decodeReuseBitmap(String fileName, Bitmap dest) throws IOException {
        nativeDecodeBitmap(fileName, dest);
    }

    public static void decodeReuseBitmapScaled(String fileName, Bitmap dest) throws IOException {
        nativeDecodeBitmapScaled(fileName, dest);
    }

    public static void decodeReuseBitmap(byte[] src, Bitmap dest) throws IOException {
        nativeDecodeArray(src, dest);
    }

    private static native void nativeDecodeBitmapScaled(String fileName, Bitmap bitmap) throws IOException;

    private static native void nativeDecodeBitmap(String fileName, Bitmap bitmap) throws IOException;

    private static native void nativeDecodeArray(byte[] array, Bitmap bitmap) throws IOException;

    private static native void nativeDecodeBitmapBlend(String fileName, Bitmap bitmap) throws IOException;
}
