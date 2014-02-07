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

    private static final int CACHE_SIZE = 10;
    private static final int CACHE_FREE_SIZE = 10;

    private static boolean USE_FREE_CACHE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private HashMap<String, Holder> references = new HashMap<String, Holder>();

    private LruCache<String, Holder> avatarCache = new LruCache<String, Holder>(CACHE_SIZE) {
        @Override
        protected void entryRemoved(boolean evicted, String key, Holder oldValue, Holder newValue) {
            if (evicted) {
                Bitmap bitmap = oldValue.sourceBitmap.get();
                if (bitmap == null) {
                    return;
                }
                if (!bitmap.isMutable()) {
                    return;
                }
                synchronized (freeBitmaps) {
                    HashSet<Bitmap> bitmaps = freeBitmaps.get(oldValue.size);
                    if (bitmaps == null) {
                        bitmaps = new HashSet<Bitmap>();
                        bitmaps.add(bitmap);
                        // Logger.d(TAG, "Adding to free cache " + key);
                        freeBitmaps.put(oldValue.size, bitmaps);
                    } else {
                        if (bitmaps.size() < CACHE_FREE_SIZE) {
                            // Logger.d(TAG, "Adding to free cache " + key);
                            bitmaps.add(bitmap);
                        }
                    }
                }
            }
        }
    };

    private HashMap<Integer, HashSet<Bitmap>> freeBitmaps = new HashMap<Integer, HashSet<Bitmap>>();

    public void putToCache(String key, int size, Bitmap bitmap) {
        if (references.get(key) == null && avatarCache.get(key) == null) {
            avatarCache.put(key, new Holder(key, size, bitmap));
        }
    }

    public Bitmap findFree(int size) {
        synchronized (freeBitmaps) {
            HashSet<Bitmap> freeSet = freeBitmaps.get(size);
            if (freeSet == null || freeSet.size() == 0) {
                return null;
            }
            Bitmap res = freeSet.iterator().next();
            freeSet.remove(res);
            return res;
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
            holder.sourceStrongBitmap = holder.sourceBitmap.get();
            if (holder.sourceStrongBitmap == null) {
                return;
            }

            boolean isMoved = false;
            if (references.containsKey(holder.key)) {
                isMoved = true;
                // Logger.d(TAG, "Move to weak cache -> " + holder.key);
                references.remove(holder.key);
            }
            if (!isMoved && avatarCache.get(holder.key) == null) {
                // Logger.d(TAG, "Adding to weak cache -> " + holder.key);
            }
            avatarCache.put(holder.key, holder);
        } else {

            holder.sourceStrongBitmap = null;

            boolean isMoved = false;
            if (avatarCache.get(holder.key) != null) {
                isMoved = true;
                // Logger.d(TAG, "Move to strong cache -> " + holder.key);
                avatarCache.remove(holder.key);
            }
            if (!isMoved && !references.containsKey(holder.key)) {
                // Logger.d(TAG, "Adding to strong cache -> " + holder.key);
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

        // Logger.d(TAG, "incReference -> " + key);

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        holder.referenceCount++;

        updateHolder(holder);
    }

    public void decReference(String key) {

        // Logger.d(TAG, "decReference -> " + key);

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
        public Bitmap sourceStrongBitmap;
        public int referenceCount;
        public int size;

        private Holder(String key, int size, Bitmap bitmap) {
            this.size = size;
            this.key = key;
            this.sourceBitmap = new SoftReference<Bitmap>(bitmap);
        }
    }
}
