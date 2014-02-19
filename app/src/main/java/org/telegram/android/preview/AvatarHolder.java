package org.telegram.android.preview;

import android.graphics.Bitmap;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 18.02.14.
 */
public class AvatarHolder {
    private static final String TAG = "ImageCache";
    private BitmapHolder bitmap;
    private AvatarLoader loader;
    private boolean isReleased = false;

    public AvatarHolder(BitmapHolder bitmap, AvatarLoader loader) {
        this.bitmap = bitmap;
        this.loader = loader;
        loader.getImageCache().incReference(bitmap.getKey(), this);
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
