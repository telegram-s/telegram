package org.telegram.android.media;

import android.graphics.Bitmap;

/**
 * Created by ex3ndr on 07.02.14.
 */
public class BitmapDecoderEx {

    static {
        System.loadLibrary("timg");
    }

    private BitmapDecoderEx() {
    }


    public static void decodeReuseBitmap(String fileName, Bitmap dest) {
        new BitmapDecoderEx().nativeDecodeBitmap(fileName, dest);
    }

    private native void nativeDecodeBitmap(String fileName, Bitmap bitmap);
}
