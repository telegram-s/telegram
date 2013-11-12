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

package com.extradea.framework.persistence.cache;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 01.12.12
 * Time: 1:59
 */
public class TimedCache<T> {
    private class CacheItem<T> {
        public T item;
        public int addedTime;

        public CacheItem(T item) {
            addedTime = (int) (new Date().getTime() / 1000);
            this.item = item;
        }

        public boolean isTimedOut() {
            int currentTime = (int) (new Date().getTime() / 1000);
            return currentTime - addedTime > itemTimeout;
        }
    }

    private final int itemTimeout;
    private HashMap<String, CacheItem<T>> cache;

    public TimedCache(int itemTimeout) {
        this.itemTimeout = itemTimeout;
        cache = new HashMap<String, CacheItem<T>>();
    }

    public void putToCache(String key, T obj) {
        if (cache.containsKey(key)) {
            cache.remove(key);
        }
        cache.put(key, new CacheItem<T>(obj));
    }

    public T fetchFromCache(String key) {
        if (cache.containsKey(key)) {
            CacheItem<T> item = cache.get(key);
            if (item.isTimedOut()) {
                cache.remove(key);
                return null;
            } else {
                return item.item;
            }
        } else {
            return null;
        }
    }

    public void clear() {
        cache.clear();
    }

    public void removeFromCache(String key) {
        cache.remove(key);
    }
}