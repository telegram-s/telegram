package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import com.actionbarsherlock.R;
import org.telegram.android.log.Logger;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class ImageCache {

    private static final int DEFAULT_CACHE_SIZE = 10;
    private static final int DEFAULT_CACHE_FREE_SIZE = 10;
    private static final boolean USE_REFERENCE_TRACK = true;

    private HashMap<String, Holder> references;
    private HashMap<String, Integer> movedBitmaps;

    private LruCache<String, Holder> avatarCache;

    private HashMap<Integer, HashSet<Bitmap>> freeBitmaps;

    private final int cacheSize;
    private final int cacheFreeSize;

    public ImageCache(int _cacheSize, int _cacheFreeSize) {
        this.cacheSize = _cacheSize;
        this.cacheFreeSize = _cacheFreeSize;

        avatarCache = new LruCache<String, Holder>(cacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Holder oldValue, Holder newValue) {
                if (evicted) {
                    BitmapHolder bitmap = oldValue.sourceBitmap.get();
                    if (bitmap == null) {
                        return;
                    }
                    if (!bitmap.getBitmap().isMutable()) {
                        return;
                    }
                    synchronized (freeBitmaps) {
                        HashSet<Bitmap> bitmaps = freeBitmaps.get(oldValue.size);
                        if (bitmaps == null) {
                            bitmaps = new HashSet<Bitmap>();
                            bitmaps.add(bitmap.getBitmap());
                            freeBitmaps.put(oldValue.size, bitmaps);
                        } else {
                            if (bitmaps.size() < cacheFreeSize) {
                                bitmaps.add(bitmap.getBitmap());
                            }
                        }
                    }
                }
            }
        };
        freeBitmaps = new HashMap<Integer, HashSet<Bitmap>>();

        references = new HashMap<String, Holder>();
    }

    public ImageCache() {
        this(DEFAULT_CACHE_SIZE, DEFAULT_CACHE_FREE_SIZE);
    }

    protected int getCacheMaxSize(int size) {
        return 10;
    }

    public void putToCache(int size, BitmapHolder bitmap) {
        if (references.get(bitmap.getKey()) == null && avatarCache.get(bitmap.getKey()) == null) {
            avatarCache.put(bitmap.getKey(), new Holder(bitmap.getKey(), size, bitmap));
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
        if (holder.references.size() <= 0) {
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
            BitmapHolder img = holder.sourceBitmap.get();
            if (img == null) {
                removeHolder(holder);
                return null;
            }
        }

        return holder;
    }

    public void incReference(String key, Object referent) {

        // Logger.d(TAG, "incReference -> " + key);

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        if (holder.references.add(referent)) {
            Logger.d("BIND", "Binded " + referent + " key " + key);
        }

        updateHolder(holder);
    }

    public void decReference(String key, Object referent) {

        // Logger.d(TAG, "decReference -> " + key);

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        if (holder.references.remove(referent)) {
            Logger.d("BIND", "Unbinded " + referent + " key " + key);
        }


        updateHolder(holder);
    }

    public BitmapHolder getFromCache(String key) {
        Holder holder = findHolder(key);
        if (holder == null) {
            return null;
        }

        return holder.sourceBitmap.get();
    }

    private class Holder {
        public String key;
        public SoftReference<BitmapHolder> sourceBitmap;
        public BitmapHolder sourceStrongBitmap;
        public HashSet<Object> references;
        public int size;

        private Holder(String key, int size, BitmapHolder bitmap) {
            this.size = size;
            this.key = key;
            this.sourceBitmap = new SoftReference<BitmapHolder>(bitmap);
            this.references = new HashSet<Object>();
        }
    }
}
