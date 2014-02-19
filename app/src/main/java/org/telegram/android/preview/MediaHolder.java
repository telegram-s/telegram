package org.telegram.android.preview;

import android.graphics.Bitmap;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 18.02.14.
 */
public class MediaHolder {
    private static final String TAG = "ImageCache";
    private BitmapHolder bitmap;
    private MediaLoader loader;
    private boolean isReleased = false;

    public MediaHolder(BitmapHolder bitmap, MediaLoader loader) {
        this.bitmap = bitmap;
        this.loader = loader;
        loader.getImageCache().incReference(bitmap.getKey(), this);
    }

    public int getW() {
        if (isReleased) {
            throw new UnsupportedOperationException();
        }
        return bitmap.getRealW();
    }

    public int getH() {
        if (isReleased) {
            throw new UnsupportedOperationException();
        }
        return bitmap.getRealH();
    }

    public Bitmap getBitmap() {
        if (isReleased) {
            throw new UnsupportedOperationException();
        }
        return bitmap.getBitmap();
    }

    public void release() {
        if (isReleased) {
            throw new UnsupportedOperationException();
        }
        if (ImageCache.IS_LOGGING) {
            Logger.d(TAG, "Releasing holder " + bitmap.getKey() + ":" + bitmap);
        }
        isReleased = true;
        loader.getImageCache().decReference(bitmap.getKey(), this);
        bitmap = null;
    }
}
