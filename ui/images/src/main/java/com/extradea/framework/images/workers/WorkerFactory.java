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

package com.extradea.framework.images.workers;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.07.12
 * Time: 7:04
 */
public class WorkerFactory {
    public static ImageWorker[] createDefaultWorkers(Context context) {
        return createDefaultWorkers(context, new ImageWorker[0]);
    }

    public static ImageWorker[] createDefaultWorkers(Context context, ImageWorker... additional) {
        ArrayList<ImageWorker> res = new ArrayList<ImageWorker>();
        res.add(new DownloadWorker());
        res.add(new DownloadWorker());
        res.add(new FileSystemWorker(context));
        res.add(new QuadCornersWorker());
        res.add(new CornersWorker());
        res.add(new CustomWorker());
        res.add(new ScaleWorker());
        for (ImageWorker worker : additional) {
            res.add(worker);
        }
        return res.toArray(new ImageWorker[0]);
    }
}