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

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User: Stepan (aka Ex3NDR) Korshakov
 * Date: 23.08.11
 * Time: 12:06
 */
public class ContextPersistence extends PersistenceObject {

    protected transient Context context;
    private transient boolean ignoreVersionCheck = false;

    public ContextPersistence(Context context) {
        this.context = context;
    }

    public ContextPersistence(Context context, boolean ignoreVersionCheck) {
        this.context = context;
        this.ignoreVersionCheck = ignoreVersionCheck;
    }

    @Override
    protected boolean ignoreVersionCheck() {
        return ignoreVersionCheck;
    }

    @Override
    protected OutputStream openWrite(String path) throws FileNotFoundException {
        return context.openFileOutput(path, Context.MODE_PRIVATE);
    }

    @Override
    protected InputStream openRead(String path, boolean error) throws IOException {
        return context.openFileInput(path);
    }
}
