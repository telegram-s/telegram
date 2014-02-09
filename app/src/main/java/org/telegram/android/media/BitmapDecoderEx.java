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


    public static void decodeReuseBitmap(String fileName, Bitmap dest) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inMutable = true;
//            options.inSampleSize = 1;
//            options.inBitmap = dest;
//            Bitmap res = BitmapFactory.decodeFile(fileName, options);
//            res.toString();
//            return;
//        }
        new BitmapDecoderEx().nativeDecodeBitmap(fileName, dest);
    }

    public static void decodeReuseBitmap(byte[] src, Bitmap dest) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inMutable = true;
//            options.inSampleSize = 1;
//            options.inBitmap = dest;
//            BitmapFactory.decodeByteArray(src, 0, src.length, options);
//            return;
//        }
        new BitmapDecoderEx().nativeDecodeArray(src, dest);
    }

    private native void nativeDecodeBitmap(String fileName, Bitmap bitmap);

    private native void nativeDecodeArray(byte[] array, Bitmap bitmap);
}
