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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.VideoThumbTask;
import com.extradea.framework.images.utils.ImageUtils;
import com.extradea.framework.images.tasks.FileSystemImageTask;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.tasks.UriImageTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.07.12
 * Time: 6:48
 */
public class FileSystemWorker implements ImageWorker {
    private Context context;

    public FileSystemWorker(Context context) {
        this.context = context;
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof FileSystemImageTask || task instanceof UriImageTask || task instanceof VideoThumbTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        if (task instanceof FileSystemImageTask) {
            final FileSystemImageTask fsTask = (FileSystemImageTask) task;
//            if (fsTask.hasSizeLimitation()) {
//                Bitmap image = controller.getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
//                    @Override
//                    public Bitmap call() throws Exception {
//                        if (fsTask.isFillRect()) {
//                            return ImageUtils.getOptimalBitmapFill(fsTask.getFileName(), fsTask.getMaxWidth(), fsTask.getMaxHeight());
//                        } else {
//                            return ImageUtils.getOptimalBitmap(fsTask.getFileName(), fsTask.getMaxWidth(), fsTask.getMaxHeight());
//                        }
//                    }
//                });
//
//                if (image == null) {
//                    return RESULT_FAILURE;
//                }
//                task.setResult(image);
//                return RESULT_OK;
//            } else {
            Bitmap image = controller.getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
                @Override
                public Bitmap call() throws Exception {
                    return ImageUtils.getOptimalBitmap(fsTask.getFileName());
                }
            });
            if (image == null) {
                return RESULT_FAILURE;
            }
            task.setResult(image);
            return RESULT_OK;
//            }
        } else if (task instanceof UriImageTask) {
            final UriImageTask uriImageTask = (UriImageTask) task;
            try {
//                if (uriImageTask.hasSizeLimitation()) {
//                    Bitmap image = controller.getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
//                        @Override
//                        public Bitmap call() throws Exception {
//                            if (uriImageTask.isFillRect()) {
//                                return ImageUtils.getOptimalBitmapFill(Uri.parse(uriImageTask.getUri()), context, uriImageTask.getMaxWidth(), uriImageTask.getMaxHeight());
//                            } else {
//                                return ImageUtils.getOptimalBitmap(Uri.parse(uriImageTask.getUri()), context, uriImageTask.getMaxWidth(), uriImageTask.getMaxHeight());
//                            }
//                        }
//                    });
//                    if (image == null) {
//                        return RESULT_FAILURE;
//                    }
//                    task.setResult(image);
//                    return RESULT_OK;
//                } else {
                Bitmap image = controller.getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
                    @Override
                    public Bitmap call() throws Exception {
                        return ImageUtils.getOptimalBitmap(Uri.parse(uriImageTask.getUri()), context);
                    }
                });
                if (image == null) {
                    return RESULT_FAILURE;
                }
                task.setResult(image);
                return RESULT_OK;
//                }
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_FAILURE;
            }

        } else {
            try {
                VideoThumbTask videoThumbTask = (VideoThumbTask) task;
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoThumbTask.getFileName());
                Bitmap res = retriever.getFrameAtTime(0);
                task.setResult(res);
                return RESULT_OK;
            } catch (Exception e) {
                e.printStackTrace();
                return RESULT_FAILURE;
            }
        }
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}