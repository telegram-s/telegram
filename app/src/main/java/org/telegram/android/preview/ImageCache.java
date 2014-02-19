package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import org.telegram.android.log.Logger;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class ImageCache {

    private static final String TAG = "ImageCache";
    public static final boolean IS_LOGGING = false;

    private static final int DEFAULT_CACHE_SIZE = 10;
    private static final int DEFAULT_CACHE_FREE_SIZE = 10;
    private static final boolean USE_REFERENCE_TRACK = true;

    private HashMap<String, Holder> references;
    private HashMap<String, Integer> movedBitmaps;

    private LruCache<String, Holder> lruCache;

    private HashMap<Integer, HashSet<Bitmap>> freeBitmaps;

    private final int cacheSize;
    private final int cacheFreeSize;

    public ImageCache(int _cacheSize, int _cacheFreeSize) {
        this.cacheSize = _cacheSize;
        this.cacheFreeSize = _cacheFreeSize;

        lruCache = new LruCache<String, Holder>(cacheSize) {
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
                            freeBitmaps.put(oldValue.size, bitmaps);
                        }

                        if (IS_LOGGING) {
                            Logger.d(TAG, "Moving to free cache: " + bitmap.getBitmap() + ", " + oldValue.key);
                        }
                        bitmaps.add(bitmap.getBitmap());
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

    public synchronized void putToCache(int size, BitmapHolder bitmap, Object referent) {
        if (IS_LOGGING) {
            Logger.d(TAG, "Adding to cache " + bitmap.getKey() + " reference #" + bitmap.getBitmap());
        }


        Holder holder = new Holder(bitmap.getKey(), size, bitmap);
        holder.references.add(referent);

        if (lruCache.get(holder.key) != null) {
            Logger.w(TAG, "Was in lru cache");
        }
        if (references.get(holder.key) != null) {
            Logger.w(TAG, "Was in reference cache");
        }

        lruCache.remove(holder.key);
        references.remove(holder.key);
        putHolder(holder);
    }

    public synchronized void putFree(Bitmap bmp, int size) {
        synchronized (freeBitmaps) {
            HashSet<Bitmap> freeSet = freeBitmaps.get(size);
            if (freeSet == null) {
                freeSet = new HashSet<Bitmap>();
                freeBitmaps.put(size, freeSet);
            }
            freeSet.add(bmp);
        }
    }

    public synchronized Bitmap findFree(int size) {
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

    private synchronized void removeHolder(Holder holder) {
        lruCache.remove(holder.key);
        references.remove(holder.key);
    }

    private synchronized void updateHolder(Holder holder) {
        putHolder(holder);
    }

    private synchronized void putHolder(Holder holder) {
        if (holder.references.size() <= 0) {
            holder.sourceStrongBitmap = holder.sourceBitmap.get();
            if (holder.sourceStrongBitmap == null) {
                return;
            }

            boolean isMoved = false;
            if (references.containsKey(holder.key)) {
                isMoved = true;
                if (IS_LOGGING) {
                    Logger.d(TAG, "Move to weak cache -> " + holder.key);
                }
                references.remove(holder.key);
            }
            if (IS_LOGGING) {
                if (!isMoved && lruCache.get(holder.key) == null) {
                    Logger.d(TAG, "Adding to weak cache -> " + holder.key);
                }
            }
            lruCache.put(holder.key, holder);
        } else {
            boolean isMoved = false;
            if (lruCache.get(holder.key) != null) {
                isMoved = true;
                if (IS_LOGGING) {
                    Logger.d(TAG, "Move to strong cache -> " + holder.key + ", #" + holder.sourceStrongBitmap.getBitmap());
                }
                lruCache.remove(holder.key);
            }
            if (IS_LOGGING) {
                if (!isMoved && !references.containsKey(holder.key)) {
                    Logger.d(TAG, "Adding to strong cache -> " + holder.key);
                }
            }
            references.put(holder.key, holder);

            holder.sourceStrongBitmap = null;
        }
    }

    private synchronized Holder findHolder(String key) {
        Holder holder = references.get(key);
        if (holder == null) {
            holder = lruCache.get(key);
        }

        if (holder != null) {
            BitmapHolder img = holder.sourceBitmap.get();
            if (img == null) {
                Logger.w(TAG, "Reference counter is broken! Bitmap garbage collected: " + holder.key);
                removeHolder(holder);
                return null;
            }
        }

        return holder;
    }

    public synchronized void incReference(String key, Object referent) {
        if (IS_LOGGING) {
            Logger.d(TAG, "incReference -> " + key + ", referent:" + referent);
        }

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        holder.references.add(referent);

        updateHolder(holder);
    }

    public synchronized void decReference(String key, Object referent) {
        if (IS_LOGGING) {
            Logger.d(TAG, "decReference -> " + key + ", referent:" + referent);
        }

        Holder holder = findHolder(key);
        if (holder == null) {
            return;
        }

        holder.references.remove(referent);

        updateHolder(holder);
    }

    public synchronized BitmapHolder getFromCache(String key) {
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
