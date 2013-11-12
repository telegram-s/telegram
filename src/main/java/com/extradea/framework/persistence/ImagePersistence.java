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

package com.extradea.framework.persistence;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * User: Stepan (aka Ex3NDR) Korshakov
 * Date: 13.09.11
 * Time: 15:41
 */
public abstract class ImagePersistence {

    public abstract Date creationDate(String id);

    public abstract void remove(String id);

    public abstract Bitmap loadImage(String id);

    public abstract void saveImage(Bitmap image, String id);
    public abstract void saveImage(byte[] image, String id);
}
