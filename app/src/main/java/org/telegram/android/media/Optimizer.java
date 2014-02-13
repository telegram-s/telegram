package org.telegram.android.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import com.extradea.framework.images.utils.ImageUtils;
import org.telegram.android.log.Logger;
import org.telegram.android.util.CustomBufferedInputStream;
import org.telegram.android.util.IOUtils;
import org.telegram.android.util.ImageNativeUtils;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 09.08.13 11:29
 */
public class Optimizer {

    private static final String TAG = "Optimizer";

    private static final int MAX_PIXELS = 1200 * 1200;
    private static final int MAX_PIXELS_HQ = 1500 * 1500;

    private static ThreadLocal<byte[]> bitmapTmp = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[16 * 1024];
        }
    };

    // Public methods

    public static void optimize(String srcFile, String destFile) throws IOException {
        optimize(new FileSource(srcFile), destFile);
    }

    public static void optimize(String uri, Context context, String destFile) throws IOException {
        optimize(new UriSource(Uri.parse(uri), context), destFile);
    }

    public static Bitmap optimize(String srcFile) throws IOException {
        return optimize(new FileSource(srcFile));
    }

    public static Bitmap optimize(String uri, Context context) throws IOException {
        return optimize(new UriSource(Uri.parse(uri), context));
    }

    public static void optimizeHQ(String srcFile, String destFile) throws IOException {
        optimizeHQ(new FileSource(srcFile), destFile);
    }

    public static void optimizeHQ(String uri, Context context, String destFile) throws IOException {
        optimizeHQ(new UriSource(Uri.parse(uri), context), destFile);
    }


    public static byte[] save(Bitmap src) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
        byte[] res = outputStream.toByteArray();
        outputStream.close();
        return res;
    }

    public static void save(Bitmap src, String destFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(destFile);
        src.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
        outputStream.close();
        src.recycle();
    }

    public static Bitmap load(String fileName) throws IOException {
        return load(new FileSource(fileName));
    }

    public static Bitmap load(byte[] data) throws IOException {
        return load(new ByteSource(data));
    }

    public static Bitmap load(String uri, Context context) throws IOException {
        return load(new UriSource(Uri.parse(uri), context));
    }

    public static BitmapInfo loadTo(String fileName, Bitmap dest) throws IOException {
        return loadTo(new FileSource(fileName), dest);
    }

    public static BitmapInfo loadTo(byte[] bytes, Bitmap dest) throws IOException {
        return loadTo(new ByteSource(bytes), dest);
    }

    public static BitmapInfo getInfo(String fileName) throws IOException {
        return getInfo(new FileSource(fileName));
    }

    public static BitmapInfo getInfo(Uri uri, Context context) throws IOException {
        return getInfo(new UriSource(uri, context));
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

    public static Bitmap scaleForMinimumSize(Bitmap src, int w, int h) {
        if (src.isRecycled()) {
            throw new IllegalStateException("Source bitmap is recycled");
        }

        if (src.getWidth() >= w && src.getHeight() >= h) {
            return src;
        }
        float scale = 1.0f;
        if (src.getWidth() < w) {
            scale = Math.max(scale, w / (float) src.getWidth());
        }
        if (src.getHeight() < h) {
            scale = Math.max(scale, h / (float) src.getHeight());
        }

        int nw = (int) (scale * src.getWidth());
        int nh = (int) (scale * src.getHeight());
        return Bitmap.createScaledBitmap(src, nw, nh, true);
    }

    public static void scaleTo(Bitmap src, Bitmap dest) {
        float ratioX = dest.getWidth() / (float) src.getWidth();
        float ratioY = dest.getHeight() / (float) src.getHeight();
        float middleX = dest.getWidth() / 2.0f;
        float middleY = dest.getHeight() / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);
        dest.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(dest);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(src, middleX - src.getWidth() / 2, middleY - src.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
    }

    public static int[] scaleToRatio(Bitmap src, int sourceW, int sourceH, Bitmap dest) {
        float ratio = Math.min(dest.getWidth() / (float) sourceW, dest.getHeight() / (float) sourceH);

        dest.eraseColor(Color.TRANSPARENT);
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(src,
                new Rect(0, 0, sourceW, sourceH),
                new Rect(0, 0, (int) (sourceW * ratio), (int) (sourceH * ratio)),
                new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
        return new int[]{(int) (sourceW * ratio), (int) (sourceH * ratio)};
    }

    public static void drawTo(Bitmap src, Bitmap dest) {
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(src, 0, 0, new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG));
    }

    public static void blur(Bitmap src) {
        ImageNativeUtils.performBlur(src);
    }

    // Private methods

    private static Bitmap load(Source source) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();

        o.inSampleSize = 1;
        o.inScaled = false;
        o.inTempStorage = bitmapTmp.get();

        if (Build.VERSION.SDK_INT >= 10) {
            o.inPreferQualityOverSpeed = true;
        }

        if (Build.VERSION.SDK_INT >= 11) {
            o.inMutable = true;
        }

        InputStream stream = createStream(source);
        try {

            return BitmapFactory.decodeStream(stream, null, o);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static int getScale(Source source) throws IOException {
        InputStream stream = createStream(source);
        try {
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
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore this
                }
            }
        }
    }

    private static int getScaleHQ(Source source) throws IOException {
        InputStream stream = createStream(source);
        try {

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, o);

            int scale = 1;
            int scaledW = o.outWidth;
            int scaledH = o.outHeight;
            while (scaledW * scaledH > MAX_PIXELS_HQ) {
                scale *= 2;
                scaledH /= 2;
                scaledW /= 2;
            }

            Logger.d(TAG, "Image Scale = " + scale + ", width: " + o.outWidth + ", height: " + o.outHeight);

            return scale;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore this
                }
            }
        }
    }

    private static BitmapInfo getInfo(Source source) throws IOException {
        return getInfo(source, false);
    }

    private static BitmapInfo getInfo(Source source, boolean ignoreOrientation) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        o.inTempStorage = bitmapTmp.get();

        InputStream fis = createStream(source);
        try {
            BitmapFactory.decodeStream(fis, null, o);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore this
                }
            }
        }

        int w = o.outWidth;
        int h = o.outHeight;

        if (!ignoreOrientation) {
            if (!isVerticalImage(source)) {
                w = o.outHeight;
                h = o.outWidth;
            }
        }

        return new BitmapInfo(w, h, o.outMimeType);
    }

    private static Bitmap buildOptimized(Source source, int scale) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        if (scale > 1) {
            o.inSampleSize = scale;
        }

        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o.inDither = false;
        o.inScaled = false;
        o.inTempStorage = bitmapTmp.get();

        InputStream stream = createStream(source);
        try {
            return BitmapFactory.decodeStream(stream, null, o);
        } finally {
            if (stream != null) {
                try {

                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void optimize(Source source, String destFile) throws IOException {
        boolean isAnimated = detectGif(source);

        if (isAnimated) {
            copy(source, destFile);
            return;
        }

        int scale = getScale(source);

        Bitmap res = buildOptimized(source, scale);
        res = fixRotation(res, source);
        save(res, destFile);
    }

    private static Bitmap optimize(Source source) throws IOException {
        boolean isAnimated = detectGif(source);

        if (isAnimated) {
            return load(source);
        }

        int scale = getScale(source);
        Bitmap res = buildOptimized(source, scale);
        res = fixRotation(res, source);
        return res;
    }


    private static void optimizeHQ(Source source, String destFile) throws IOException {
        int scale = getScaleHQ(source);
        Bitmap res = buildOptimized(source, scale);
        res = fixRotation(res, source);
        save(res, destFile);
    }

    private static BitmapInfo loadTo(Source source, Bitmap dest) throws IOException {
        BitmapInfo res = getInfo(source, true);
        decodeReuse(source, res, dest);
        return res;
    }

    private static void decodeReuse(Source source, BitmapInfo info, Bitmap dest) throws IOException {
        if (Build.VERSION.SDK_INT >= 11 && info.getWidth() == dest.getWidth()
                && info.getHeight() == dest.getHeight()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            options.inPreferQualityOverSpeed = true;
            options.inSampleSize = 1;
            options.inScaled = false;
            options.inBitmap = dest;
            options.inTempStorage = bitmapTmp.get();

            if (source instanceof ByteSource) {
                byte[] data = ((ByteSource) source).getData();
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
            } else if (source instanceof FileSource) {
                InputStream stream = new CustomBufferedInputStream(new FileInputStream(((FileSource) source).getFileName()));
                try {
                    BitmapFactory.decodeStream(stream, null, options);
                } finally {
                    if (stream != null) {
                        try {

                            stream.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            if (source instanceof ByteSource) {
                BitmapDecoderEx.decodeReuseBitmap(((ByteSource) source).getData(), dest);
            } else if (source instanceof FileSource) {
                BitmapDecoderEx.decodeReuseBitmap(((FileSource) source).getFileName(), dest);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static InputStream createStream(Source source) throws IOException {
        if (source instanceof FileSource) {
            return new CustomBufferedInputStream(new FileInputStream(((FileSource) source).getFileName()));
        } else if (source instanceof UriSource) {
            return new CustomBufferedInputStream(
                    ((UriSource) source).getContext().getContentResolver()
                            .openInputStream(((UriSource) source).getUri()));
        } else if (source instanceof ByteSource) {
            return new ByteArrayInputStream(((ByteSource) source).getData());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void copy(Source source, String destFile) throws IOException {
        if (source instanceof FileSource) {
            IOUtils.copy(new File(((FileSource) source).getFileName()), new File(destFile));
        } else {
            InputStream stream = createStream(source);
            try {
                IOUtils.copy(stream, new File(destFile));
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    private static boolean detectGif(Source source) throws IOException {
        InputStream stream = createStream(source);
        try {
            char a = (char) stream.read();
            char b = (char) stream.read();
            char c = (char) stream.read();
            if (a == 'G' && b == 'I' && c == 'F') {
                return true;
            }
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static String getOrientationTag(String fileName) throws IOException {
        ExifInterface exif = new ExifInterface(fileName);
        return exif.getAttribute(ExifInterface.TAG_ORIENTATION);
    }

    private static boolean isVerticalImage(Source source) throws IOException {
        if (source instanceof FileSource) {
            return isVerticalImage(((FileSource) source).getFileName());
        } else if (source instanceof UriSource) {
            return isVerticalImage(((UriSource) source).getUri(), ((UriSource) source).getContext());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean isVerticalImage(String fileName) throws IOException {
        String exifOrientation = getOrientationTag(fileName);
        return (exifOrientation.equals("0") || exifOrientation.equals("1") || exifOrientation.equals("2") || exifOrientation.equals("3") || exifOrientation.equals("4"));
    }

    private static boolean isVerticalImage(Uri uri, Context context) {
        int angle = getContentRotation(uri, context);
        return angle == 0 || angle == 180;
    }

    private static int getContentRotation(Uri uri, Context context) {
        try {
            String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
            Cursor c = context.getContentResolver().query(
                    uri, projection, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static Bitmap fixRotation(Bitmap bmp, Source source) throws IOException {
        if (source instanceof FileSource) {
            return fixExifRotation(bmp, ((FileSource) source).getFileName());
        } else if (source instanceof UriSource) {
            return fixRotation(bmp, ((UriSource) source).getUri(), ((UriSource) source).getContext());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static Bitmap fixRotation(Bitmap bmp, Uri uri, Context context) throws IOException, OutOfMemoryError {
        if (bmp == null) {
            return null;
        }
        return ImageUtils.fixRotation(bmp, getContentRotation(uri, context));
    }

    private static Bitmap fixExifRotation(Bitmap bmp, String exifFileName) throws IOException, OutOfMemoryError {
        if (bmp == null)
            return null;
        if (Build.VERSION.SDK_INT >= 5) {
            int rotation = 0;
            String exifOrientation = getOrientationTag(exifFileName);
            if (exifOrientation.equals("6")) {
                rotation = 90;
            } else if (exifOrientation.equals("3")) {
                rotation = 180;
            } else if (exifOrientation.equals("8")) {
                rotation = -90;
            }

            return fixRotation(bmp, rotation);
        } else {
            return bmp;
        }
    }

    private static Bitmap fixRotation(Bitmap bmp, int angle) {
        if (angle == 0) {
            return bmp;
        }

        int destWidth = bmp.getWidth();
        int destHeight = bmp.getHeight();
        if (angle == 90) {
            destHeight = bmp.getWidth();
            destWidth = bmp.getHeight();
        } else if (angle == 180) {
            // Do nothing
        } else if (angle == -90) {
            destHeight = bmp.getWidth();
            destWidth = bmp.getHeight();
        } else {
            return bmp;
        }

        Bitmap targetBitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-bmp.getWidth() / 2, -bmp.getHeight() / 2);
        matrix.postRotate(angle);
        matrix.postTranslate(destWidth / 2, destHeight / 2);
        canvas.drawBitmap(bmp, matrix, new Paint());

        bmp.recycle();

        return targetBitmap;
    }

    private static abstract class Source {

    }

    private static class FileSource extends Source {
        private String fileName;

        private FileSource(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    private static class UriSource extends Source {
        private Uri uri;
        private Context context;

        private UriSource(Uri uri, Context context) {
            this.uri = uri;
            this.context = context;
        }

        public Uri getUri() {
            return uri;
        }

        public Context getContext() {
            return context;
        }
    }

    private static class ByteSource extends Source {
        private byte[] data;

        private ByteSource(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }

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

    public static class BitmapInfo {
        private int width;
        private int height;
        private String mimeType;

        public BitmapInfo(int width, int height, String mimeType) {
            this.width = width;
            this.height = height;
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
