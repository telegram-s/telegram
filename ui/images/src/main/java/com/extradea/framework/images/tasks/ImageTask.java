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

package com.extradea.framework.images.tasks;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.06.12
 * Time: 23:43
 */
public abstract class ImageTask implements Serializable {
    private boolean putInMemoryCache = true;
    private boolean putInDiskCache = true;

    private transient Bitmap result;

    private transient byte[] binaryResult;

    private ImageTask[] requiredTasks;

    private String key;

    private boolean forceSaveToFS;

    public ImageTask() {

    }

    public Bitmap getResult() {
        return result;
    }

    public void setResult(Bitmap result) {
        this.result = result;
    }

    public byte[] getBinaryResult() {
        return binaryResult;
    }

    public void setBinaryResult(byte[] binaryResult) {
        this.binaryResult = binaryResult;
    }

    public ImageTask[] getRequiredTasks() {
        return requiredTasks;
    }

    public void setRequiredTasks(ImageTask[] requiredTasks) {
        this.requiredTasks = requiredTasks;
    }

    public boolean isPutInMemoryCache() {
        return putInMemoryCache;
    }

    public void setPutInMemoryCache(boolean putInMemoryCache) {
        this.putInMemoryCache = putInMemoryCache;
    }

    public boolean isPutInDiskCache() {
        return putInDiskCache;
    }

    public void setPutInDiskCache(boolean putInDiskCache) {
        this.putInDiskCache = putInDiskCache;
    }

    protected abstract String getKeyImpl();

    public final String getKey() {
        if (key == null) {
            key = getKeyImpl();
        }
        return key;
    }

    @Override
    public String toString() {
        return getKey();
    }

    public boolean isForceSaveToFS() {
        return forceSaveToFS;
    }

    public void setForceSaveToFS(boolean forceSaveToFS) {
        this.forceSaveToFS = forceSaveToFS;
    }

    public boolean skipDiskCacheCheck() {
        return false;
    }
}
