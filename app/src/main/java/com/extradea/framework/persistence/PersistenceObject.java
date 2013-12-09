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

import java.io.*;
import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 * User: sexshop
 * Date: 12.01.11
 * Time: 11:54
 * To change this template use File | Settings | File Templates.
 */
public abstract class PersistenceObject implements Serializable {

    protected abstract boolean ignoreVersionCheck();

    protected abstract OutputStream openWrite(String path) throws FileNotFoundException;

    protected abstract InputStream openRead(String path, boolean error) throws IOException;

    protected InputStream openRead(String path) throws IOException {
        return openRead(path, false);
    }

    public boolean trySave() {
        try {
            save();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean tryLoad() {
        try {
            load();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void save() throws IOException, ClassNotFoundException {
        beforeSave();
        ObjectOutputStream out = new ObjectOutputStream(openWrite(getClass().getName() + ".sav"));
        out.writeObject(this);
        out.close();
        afterSave();
    }

    protected void beforeSave() {

    }

    protected void afterSave() {

    }

    private void load(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream in = ignoreVersionCheck() ? new WeakObjectInputStream(is) : new ObjectInputStream(is);
        Object data = in.readObject();
        if (data.getClass().equals(getClass())) {
            for (Field field : getClass().getDeclaredFields()) {
                field.setAccessible(true);

                try {
                    Class<?> type = field.getType();
                    if (type == Integer.TYPE) {
                        field.setInt(this, field.getInt(data));
                    } else if (type == Byte.TYPE) {
                        field.setByte(this, field.getByte(data));
                    } else if (type == Character.TYPE) {
                        field.setChar(this, field.getChar(data));
                    } else if (type == Short.TYPE) {
                        field.setShort(this, field.getShort(data));
                    } else if (type == Boolean.TYPE) {
                        field.setBoolean(this, field.getBoolean(data));
                    } else if (type == Long.TYPE) {
                        field.setLong(this, field.getLong(data));
                    } else if (type == Float.TYPE) {
                        field.setFloat(this, field.getFloat(data));
                    } else if (type == Double.TYPE) {
                        field.setDouble(this, field.getDouble(data));
                    } else {
                        field.set(this, field.get(data));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        in.close();
    }

    public void load() throws IOException, ClassNotFoundException {
        beforeLoad();
        try {
            load(openRead(getClass().getName() + ".sav", false));
        } catch (Exception e) {
            e.printStackTrace();
            load(openRead(getClass().getName() + ".sav", true));
        }
        afterLoad();
    }

    protected void beforeLoad() {

    }

    protected void afterLoad() {

    }

}