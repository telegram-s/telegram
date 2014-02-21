package org.telegram.android.preview;

import android.graphics.Bitmap;
import org.telegram.android.preview.cache.BitmapHolder;
import org.telegram.android.preview.cache.ImageCache;

/**
 * Created by ex3ndr on 21.02.14.
 */
public class ImageHolder {
    private BitmapHolder bitmap;
    private ImageCache cache;
    private boolean isReleased = false;

    public ImageHolder(BitmapHolder bitmap, ImageCache cache) {
        this.bitmap = bitmap;
        this.cache = cache;
        cache.incReference(bitmap.getKey(), this);
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
        isReleased = true;
        cache.decReference(bitmap.getKey(), this);
        bitmap = null;
    }
}
