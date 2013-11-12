package org.telegram.android.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import com.extradea.framework.images.utils.ImageUtils;
import org.telegram.android.log.Logger;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 09.08.13 11:29
 */
public class Optimizer {

    public static class FastPreviewResult {
        private byte[] data;
        private int w;
        private int h;

        public FastPreviewResult(byte[] data, int w, int h) {
            this.data = data;
            this.w = w;
            this.h = h;
        }

        public byte[] getData() {
            return data;
        }

        public int getW() {
            return w;
        }

        public int getH() {
            return h;
        }
    }

    private static final String TAG = "Optimizer";

    private static final int MAX_PIXELS = 1200 * 1200;

    private static int getScale(InputStream stream) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);

        int scale = 1;
        int scaledW = o.outWidth;
        int scaledH = o.outHeight;
        while (scaledW * scaledH > MAX_PIXELS) {
            scale *= 2;
            scaledH /= 2;
            scaledW /= 2;
        }

        Logger.d(TAG, "Image Scale = " + scale + ", width: " + o.outWidth + ", height: " + o.outHeight);

        return scale;
    }

    public static Point getSize(String fileName) throws IOException {
        InputStream fis = new FileInputStream(fileName);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();
        int w = o.outWidth;
        int h = o.outHeight;
        if (!ImageUtils.isVerticalImage(fileName)) {
            w = o.outHeight;
            h = o.outWidth;
        }
        return new Point(w, h);
    }

    public static Point getSize(Uri uri, Context context) throws IOException {
        InputStream fis = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();
        int w = o.outWidth;
        int h = o.outHeight;
        if (!ImageUtils.isVerticalImage(uri, context)) {
            w = o.outHeight;
            h = o.outWidth;
        }
        return new Point(w, h);
    }

    private static void writeOptimized(InputStream stream, int scale, String destFile) throws IOException {
        Bitmap res = buildOptimized(stream, scale);
        save(res, destFile);
    }

    private static Bitmap buildOptimized(InputStream stream, int scale) throws IOException {
        Bitmap res;
        BitmapFactory.Options o = new BitmapFactory.Options();
        if (scale > 1) {
            o.inSampleSize = scale;
        }

        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o.inDither = false;
        o.inScaled = false;
        res = BitmapFactory.decodeStream(stream, null, o);
        return res;
    }

    private static void save(Bitmap src, String destFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(destFile);
        src.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
        outputStream.close();

        src.recycle();
    }

    public static void optimize(String srcFile, String destFile) throws IOException {
        FileInputStream fis = new FileInputStream(srcFile);
        int scale = getScale(fis);
        fis.close();

        fis = new FileInputStream(srcFile);
        Bitmap res = buildOptimized(fis, scale);
        fis.close();
        res = ImageUtils.fixExifRotation(res, srcFile);
        save(res, destFile);
    }

    public static void optimize(String uri, Context context, String destFile) throws IOException {
        InputStream fis = context.getContentResolver().openInputStream(Uri.parse(uri));
        int scale = getScale(fis);
        fis.close();

        fis = context.getContentResolver().openInputStream(Uri.parse(uri));
        Bitmap res = buildOptimized(fis, scale);
        fis.close();

        res = ImageUtils.fixRotation(res, Uri.parse(uri), context);

        save(res, destFile);
    }

    public static FastPreviewResult buildPreview(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        float scale = 90.0f / Math.max(bitmap.getWidth(), bitmap.getHeight());
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true);
        bitmap.recycle();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 55, outputStream);
        int w = scaled.getWidth();
        int h = scaled.getHeight();
        scaled.recycle();
        return new FastPreviewResult(outputStream.toByteArray(), w, h);
    }

    public static FastPreviewResult buildPreview(String file) {
        return buildPreview(BitmapFactory.decodeFile(file));
    }
}
