/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//import android.media.ExifInterface;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 03.06.12
 * Time: 3:48
 */
public class ImageUtils {

    private static final int MAX_PIXELS = 1200 * 1000;

    private static final ThreadLocal<byte[]> imageCache = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[32 * 1024];
        }
    };

    private ImageUtils() {

    }

    public static byte[] getTempStorage() {
        return imageCache.get();
    }

    private static int getScale(InputStream stream) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);

        int scale = 1;
        int scaledW = o.outWidth;
        int scaledH = o.outHeight;
        while ((scaledW / 2) * (scaledH / 2) > MAX_PIXELS) {
            scale *= 2;
            scaledH /= 2;
            scaledW /= 2;
        }
        return scale;
    }

    private static int getScale(InputStream stream, int maxW, int maxH) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);

        int scale = 1;
        int scaledW = o.outWidth;
        int scaledH = o.outHeight;
        while ((scaledW / 2) > maxW || (scaledH / 2) > maxH) {
            scale *= 2;
            scaledH /= 2;
            scaledW /= 2;
        }
        return scale;
    }

    private static Bitmap buildBitmap(InputStream in, int scale) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o.inDither = false;
        o.inScaled = false;
        o.inSampleSize = scale;
        o.inTempStorage = imageCache.get();
        if (scale > 1) {
            return BitmapFactory.decodeStream(in, null, o);
        } else {
            return BitmapFactory.decodeStream(in);
        }
    }

    public static Bitmap scale(Bitmap src, int destW, int destH) {
        return scale(src, destW, destH, false);
    }

    public static Bitmap scale(Bitmap src, int destW, int destH, boolean autoRecycle) {
        if (src == null) {
            return null;
        }

        if (src.getWidth() == destW && src.getHeight() == destH) {
            return src;
        }

        float scaleW = destW / (float) src.getWidth();
        float scaleH = destH / (float) src.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleW, scaleH);
        Bitmap scaledBitmap = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        if (autoRecycle) {
            src.recycle();
        }
        return scaledBitmap;
    }

    public static Bitmap thumb(Bitmap src, int destW, int destH, boolean autoRecycle) {
        if (src == null)
            return null;

        if (src.getWidth() == destW && src.getHeight() == destH) {
            return src;
        }

        float scale = Math.max(destW / (float) src.getWidth(), destH / (float) src.getHeight());
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate(destW - (src.getWidth() * scale) / 2, destH - (src.getHeight() * scale) / 2);
        Bitmap res = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        if (autoRecycle) {
            src.recycle();
        }
        return res;
    }

    private static Bitmap getPreScaled(Uri uri, Context context, int maxWidth, int maxHeight) throws IOException {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            int scale = getScale(in, maxWidth, maxHeight);
            in.close();
            in = context.getContentResolver().openInputStream(uri);
            return buildBitmap(in, scale);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Bitmap getPreScaled(String fileName, int maxWidth, int maxHeight) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(fileName);
            int scale = getScale(in, maxWidth, maxHeight);
            in.close();
            in = new FileInputStream(fileName);
            return buildBitmap(in, scale);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Bitmap getPreScaled(Uri uri, Context context) throws IOException {
        InputStream in = null;
        try {
            in = context.getContentResolver().openInputStream(uri);
            int scale = getScale(in);
            in.close();
            in = context.getContentResolver().openInputStream(uri);
            return buildBitmap(in, scale);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Bitmap getPreScaled(String fileName) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(fileName);
            int scale = getScale(in);
            in.close();
            in = new FileInputStream(fileName);
            return buildBitmap(in, scale);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static Bitmap getOptimalBitmapFill(Uri uri, Context context, int maxWidth, int maxHeight) throws OutOfMemoryError {
        int w = maxWidth;
        int h = maxHeight;
        if (!isVerticalImage(uri, context)) {
            h = maxWidth;
            w = maxHeight;
        }
        try {
            Bitmap preScaled = getPreScaled(uri, context, w, h);
            Bitmap scaled = scale(preScaled, w, h);
            if (scaled != preScaled) {
                preScaled.recycle();
            }
            return fixRotation(scaled, uri, context);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOptimalBitmapFill(String path, int maxWidth, int maxHeight) throws OutOfMemoryError {
        int w = maxWidth;
        int h = maxHeight;
        if (!isVerticalImage(path)) {
            h = maxWidth;
            w = maxHeight;
        }
        try {
            Bitmap preScaled = getPreScaled(path, w, h);
            Bitmap scaled = scale(preScaled, w, h);
            if (scaled != preScaled) {
                preScaled.recycle();
            }
            return fixExifRotation(scaled, path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getBitmapThumb(String path, int maxWidth, int maxHeight) throws OutOfMemoryError {
        int w = maxWidth;
        int h = maxHeight;
        if (!isVerticalImage(path)) {
            h = maxWidth;
            w = maxHeight;
        }
        try {
            return fixExifRotation(thumb(getPreScaled(path, w, h), w, h, true), path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOptimalBitmap(String path, int maxWidth, int maxHeight) throws OutOfMemoryError {
        int w = maxWidth;
        int h = maxHeight;
        if (!isVerticalImage(path)) {
            h = maxWidth;
            w = maxHeight;
        }
        try {
            return fixExifRotation(getPreScaled(path, w, h), path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOptimalBitmap(Uri uri, Context context, int maxWidth, int maxHeight) throws OutOfMemoryError {
        int w = maxWidth;
        int h = maxHeight;
        if (!isVerticalImage(uri, context)) {
            h = maxWidth;
            w = maxHeight;
        }
        try {
            return fixRotation(getPreScaled(uri, context, w, h), uri, context);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOptimalBitmap(String path) throws OutOfMemoryError {
        try {
            return fixExifRotation(getPreScaled(path), path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getOptimalBitmap(Uri uri, Context context) throws OutOfMemoryError {
        try {
            return getPreScaled(uri, context);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getContentRotation(Uri uri, Context context) {
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

    public static String getOrientationTag(String fileName) {
        try {
            //ExifInterface exif = new ExifInterface(exifFileName);
            //String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            Class clazz = Class.forName("android.media.ExifInterface");
            Object exifObj = clazz.getConstructor(String.class).newInstance(fileName);
            return (String) clazz.getMethod("getAttribute", String.class).invoke(exifObj, "Orientation");
        } catch (Exception e) {
            Log.e("ImageUtils", e.getMessage(), e);
        }
        return "0";
    }

    public static boolean isVerticalImage(String fileName) {
        String exifOrientation = getOrientationTag(fileName);
        return (exifOrientation.equals("0") || exifOrientation.equals("1") || exifOrientation.equals("2") || exifOrientation.equals("3") || exifOrientation.equals("4"));
    }

    public static boolean isVerticalImage(Uri uri, Context context) {
        int angle = getContentRotation(uri, context);
        return angle == 0 || angle == 180;
    }

    public static Bitmap fixRotation(Bitmap bmp, int angle) {
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

    public static Bitmap fixRotation(Bitmap bmp, Uri uri, Context context) throws IOException, OutOfMemoryError {
        if (bmp == null) {
            return null;
        }
        return ImageUtils.fixRotation(bmp, getContentRotation(uri, context));
    }

    public static Bitmap fixExifRotation(Bitmap bmp, String exifFileName) throws IOException, OutOfMemoryError {
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
}