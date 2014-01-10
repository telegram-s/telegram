/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images.cache;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 02.06.12
 * Time: 0:53
 */
public class LruBitmapCache implements BitmapCache {

    private LruCache<String, Bitmap> imageCache;
    private HashMap<String, WeakReference<Bitmap>> imageTempCache;

    public LruBitmapCache(int size) {
        imageTempCache = new HashMap<String, WeakReference<Bitmap>>();
        imageCache = new LruCache<String, Bitmap>(size) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (Build.VERSION.SDK_INT >= 12) {
                    return value.getByteCount();
                } else {
                    return value.getRowBytes() * value.getHeight();
                }
            }
        };
    }

    @Override
    public void cache(String key, Bitmap value) {
        imageCache.put(key, value);
        imageTempCache.put(key, new WeakReference<Bitmap>(value));
    }

    @Override
    public Bitmap take(String key) {
        Bitmap res = imageCache.get(key);
        if (res == null) {
            WeakReference<Bitmap> reference = imageTempCache.get(key);
            if (reference != null) {
                return reference.get();
            }
        }
        return res;
    }

    @Override
    public int getCacheSize() {
        return imageTempCache.size();
    }

    @Override
    public void clear() {
        imageCache.evictAll();
        System.gc();
        System.runFinalization();
        System.gc();
        System.gc();
    }

    @Override
    public void removeFromCache(String key) {
        Bitmap bitmap = take(key);
        imageTempCache.remove(key);
        imageCache.remove(key);
        if (bitmap != null) {
            bitmap.recycle();
        }
    }
}
