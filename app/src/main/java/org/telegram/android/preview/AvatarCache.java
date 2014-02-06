package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class AvatarCache {

    private static final int CACHE_SIZE = 40;
    private static final int CACHE_FREE_SIZE = 10;

    private static boolean USE_FREE_CACHE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    private HashMap<Integer, HashSet<Bitmap>> freeCache = new HashMap<Integer, HashSet<Bitmap>>();

    private LruCache<String, Bitmap> avatarCache = new LruCache<String, Bitmap>(CACHE_SIZE);

    public void putToCache(String key, Bitmap bitmap) {
        avatarCache.put(key, bitmap);
    }

    public Bitmap getFromCache(String key) {
        return avatarCache.get(key);
    }
}
