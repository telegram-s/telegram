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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 02.06.12
 * Time: 1:00
 */
public class BitmapCacheFactory {
    public static final BitmapCache createMemoryCache(Context context) {
        // For native bitmaps android versions (< 3.0)
        int size = 1024 * 1024; //1MB

        if (Build.VERSION.SDK_INT >= 11/*Honeycomb*/) {
            try {
                int memClass = (Integer) ActivityManager.class.getMethod("getMemoryClass").invoke((context.getSystemService(
                        Context.ACTIVITY_SERVICE)));
                /*final int memClass = ((ActivityManager) context.getSystemService(
                        Context.ACTIVITY_SERVICE)).getMemoryClass();*/
                size = Math.min(8 * 1024 * 1024, (1024 * 1024 * memClass) / 4);// 25% of total memory
            } catch (Exception e) {
                Log.e("BitmapCacheFactory", e.getMessage(), e);
            }
        }

        return new LruBitmapCache(size);
    }
}
