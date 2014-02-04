package org.telegram.android.views;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import java.util.HashSet;

/**
 * Created by ex3ndr on 04.02.14.
 */
public class MediaFastCache {

    private static final int MAX_W = 90;
    private static final int MAX_H = 90;

    private static final int DEFAULT_W = 90;
    private static final int DEFAULT_H = 90;
    private static final int DEFAULT_CACHE_SIZE = 30;
    private static final int DEFAULT_CACHE_FREE_SIZE = 10;

    private static final int CUSTOM_CACHE_SIZE = 30;
    private static final int CUSTOM_CACHE_FREE_SIZE = 10;

    private static boolean USE_FREE_CACHE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private HashSet<Bitmap> customFreeCache = new HashSet<Bitmap>();
    private LruCache<Integer, Bitmap> customCache = new LruCache<Integer, Bitmap>(CUSTOM_CACHE_SIZE) {
        @Override
        protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
            if (!USE_FREE_CACHE) {
                return;
            }
            if (evicted && oldValue != null) {
                if (customFreeCache.size() < CUSTOM_CACHE_FREE_SIZE) {
                    customFreeCache.add(oldValue);
                }
            }
        }
    };

    private HashSet<Bitmap> defaultFreeCache = new HashSet<Bitmap>();
    private LruCache<Integer, Bitmap> defaultCache = new LruCache<Integer, Bitmap>(DEFAULT_CACHE_SIZE) {
        @Override
        protected void entryRemoved(boolean evicted, Integer key, Bitmap oldValue, Bitmap newValue) {
            if (!USE_FREE_CACHE) {
                return;
            }
            if (evicted && oldValue != null) {
                if (defaultFreeCache.size() < DEFAULT_CACHE_FREE_SIZE) {
                    defaultFreeCache.add(oldValue);
                }
            }
        }
    };

    public void putToCache(int key, Bitmap bitmap) {
        if (bitmap.getWidth() > MAX_W || bitmap.getHeight() > MAX_H) {
            // Ignore too big images
            return;
        }
        if (key < 0) {
            if (!bitmap.isMutable()) {
                // Ignore not mutable images
                return;
            }
            if (!USE_FREE_CACHE) {
                return;
            }
            if (bitmap.getWidth() == DEFAULT_W && bitmap.getHeight() == DEFAULT_H) {
                defaultFreeCache.add(bitmap);
            } else {
                customFreeCache.add(bitmap);
            }
        } else {
            if (bitmap.getWidth() == DEFAULT_W && bitmap.getHeight() == DEFAULT_H) {
                defaultCache.put(key, bitmap);
            } else {
                customCache.put(key, bitmap);
            }
        }
    }

    public void putToCache(Bitmap bitmap) {
        putToCache(-1, bitmap);
    }

    public Bitmap findInCache(int key) {
        Bitmap res = defaultCache.get(key);
        if (res == null) {
            res = customCache.get(key);
        }
        return res;
    }

    public Bitmap findInFreeCache(int w, int h) {
        if (w > MAX_W || h > MAX_H) {
            return null;
        }
        if (!USE_FREE_CACHE) {
            return null;
        }
        if (w == DEFAULT_W && h == DEFAULT_H) {
            if (defaultFreeCache.size() > 0) {
                Bitmap res = defaultFreeCache.iterator().next();
                defaultFreeCache.remove(res);
                return res;
            } else {
                return null;
            }
        } else {
            for (Bitmap src : customFreeCache) {
                if (src.getWidth() == w && src.getHeight() == h) {
                    customFreeCache.remove(src);
                    return src;
                }
            }
            return null;
        }
    }
}
