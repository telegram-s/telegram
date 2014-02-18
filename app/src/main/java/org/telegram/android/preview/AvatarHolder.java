package org.telegram.android.preview;

import android.graphics.Bitmap;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 18.02.14.
 */
public class AvatarHolder {
    private static final String TAG = "ImageCache";
    private String key;
    private BitmapHolder bitmap;
    private AvatarLoader loader;
    private boolean isReleased = false;

    public AvatarHolder(String key, BitmapHolder bitmap, AvatarLoader loader) {
        this.key = key;
        this.bitmap = bitmap;
        this.loader = loader;
        loader.getImageCache().incReference(key, this);
    }

    public String getKey() {
        if (isReleased) {
            throw new UnsupportedOperationException();
        }
        return key;
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
        Logger.d(TAG, "Releasing holder " + key + ":" + bitmap);
        isReleased = true;
        loader.getImageCache().decReference(key, this);
        bitmap = null;
        key = null;
    }

}
