package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import org.telegram.android.log.Logger;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class AvatarCache {

    private static final String TAG = "AvatarCache";

    private static final int CACHE_SIZE = 40;
    private static final int CACHE_FREE_SIZE = 10;

    private static boolean USE_FREE_CACHE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private HashMap<String, Holder> references = new HashMap<String, Holder>();

    private LruCache<String, Holder> avatarCache = new LruCache<String, Holder>(CACHE_SIZE);

    public void putToCache(String key, Bitmap bitmap) {
        if (references.get(key) == null && avatarCache.get(key) == null) {
            avatarCache.put(key, new Holder(key, bitmap));
        }
    }

    private void removeHolder(Holder holder) {
        avatarCache.remove(holder.key);
        references.remove(holder.key);
    }

    private void updateHolder(Holder holder) {
        putHolder(holder);
    }

    private void putHolder(Holder holder) {
        if (holder.referenceCount <= 0) {
            boolean isMoved = false;
            if (references.containsKey(holder.key)) {
                isMoved = true;
                Logger.d(TAG, "Move to weak cache -> " + holder.key);
                references.remove(holder.key);
            }
            if (!isMoved && avatarCache.get(holder.key) == null) {
                Logger.d(TAG, "Adding to weak cache -> " + holder.key);
            }
            avatarCache.put(holder.key, holder);
        } else {
            boolean isMoved = false;
            if (avatarCache.get(holder.key) != null) {
                isMoved = true;
                Logger.d(TAG, "Move to strong cache -> " + holder.key);
                avatarCache.remove(holder.key);
            }
            if (!isMoved && !references.containsKey(holder.key)) {
                Logger.d(TAG, "Adding to strong cache -> " + holder.key);
            }
            references.put(holder.key, holder);
        }
    }

    private Holder findHolder(String key) {
        Holder holder = references.get(key);
        if (holder == null) {
            holder = avatarCache.get(key);
        }

        if (holder != null) {
            Bitmap img = holder.sourceBitmap.get();
            if (img == null) {
                removeHolder(holder);
                return null;
            }
        }

        return holder;
    }

    public void incReference(String key) {

        Logger.d(TAG, "incReference -> " + key);

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        holder.referenceCount++;

        updateHolder(holder);
    }

    public void decReference(String key) {

        Logger.d(TAG, "decReference -> " + key);

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        holder.referenceCount--;

        updateHolder(holder);
    }

    public Bitmap getFromCache(String key) {
        Holder holder = findHolder(key);
        if (holder == null) {
            return null;
        }

        return holder.sourceBitmap.get();
    }

    private class Holder {
        public String key;
        public SoftReference<Bitmap> sourceBitmap;
        public int referenceCount;

        private Holder(String key, Bitmap bitmap) {
            this.key = key;
            this.sourceBitmap = new SoftReference<Bitmap>(bitmap);
        }
    }
}
