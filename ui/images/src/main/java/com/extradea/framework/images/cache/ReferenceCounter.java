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

import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.ImageReceiver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Property of Extradea LLC.
 * Author: Korshakov Stepan
 * Created: 21.04.13 1:35
 */
public class ReferenceCounter {

    private static final int MAX_ALIVE_IMAGES = 20;

    private HashMap<String, ArrayList<WeakReference<ImageReceiver>>> counters =
            new HashMap<String, ArrayList<WeakReference<ImageReceiver>>>();

    private ImageController controller;

    public ReferenceCounter(ImageController controller) {
        this.controller = controller;
    }

    public void addReference(String id, ImageReceiver receiver) {
        synchronized (counters) {
            outer:
            for (String keys : counters.keySet()) {
                ArrayList<WeakReference<ImageReceiver>> references = counters.get(keys);
                for (WeakReference<ImageReceiver> r : references) {
                    if (r.get() == receiver) {
                        references.remove(r);
                        break outer;
                    }
                }
            }

            if (!counters.containsKey(id)) {
                counters.put(id, new ArrayList<WeakReference<ImageReceiver>>());
            }

            ArrayList<WeakReference<ImageReceiver>> list = counters.get(id);
            list.add(new WeakReference<ImageReceiver>(receiver));

            /*if (getAliveImages() > MAX_ALIVE_IMAGES) {
                for (String s : getFreeImages()) {
                    counters.remove(s);
                    controller.removeFromCache(s);
                }
            }*/
        }
    }

    public void removeReference(ImageReceiver receiver) {
        synchronized (counters) {
            for (String keys : counters.keySet()) {
                ArrayList<WeakReference<ImageReceiver>> references = counters.get(keys);
                for (WeakReference<ImageReceiver> r : references) {
                    if (r.get() == receiver) {
                        references.remove(r);
                        return;
                    }
                }
            }
        }
    }

    public String[] getFreeImages() {
        synchronized (counters) {
            ArrayList<String> res = new ArrayList<String>();
            for (String keys : counters.keySet()) {
                ArrayList<WeakReference<ImageReceiver>> references = counters.get(keys);
                boolean founded = false;
                for (WeakReference<ImageReceiver> r : references) {
                    if (r.get() != null) {
                        founded = true;
                        break;
                    }
                }

                if (founded) {
                    res.add(keys);
                }
            }
            return res.toArray(new String[0]);
        }
    }

    public int getAliveImages() {
        int size = 0;
        synchronized (counters) {
            outer:
            for (String keys : counters.keySet()) {
                ArrayList<WeakReference<ImageReceiver>> references = counters.get(keys);
                for (WeakReference<ImageReceiver> r : references) {
                    if (r.get() != null) {
                        size++;
                        continue outer;
                    }
                }
            }
        }
        return size;
    }

    public int getReferenceCount(String id) {
        synchronized (counters) {
            if (counters.containsKey(id)) {
                int count = 0;
                for (WeakReference<ImageReceiver> reference : counters.get(id)) {
                    if (reference.get() != null) {
                        count++;
                    }
                }
                return count;
            }

            return 0;
        }
    }
}