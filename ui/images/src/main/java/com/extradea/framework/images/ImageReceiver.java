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

package com.extradea.framework.images;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import com.extradea.framework.images.tasks.ImageTask;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 27.06.12
 * Time: 4:00
 */
public abstract class ImageReceiver implements ImageController.ImageControllerListener, ReceiverControllerCallback {

    private static final String TAG = "ImageReceiver";

    public static final int STATUS_LOADED = 2;
    public static final int STATUS_EMPTY = 0;
    public static final int STATUS_LOADING = 1;
    public static final int STATUS_FAILURE = 1;

    private int status;

    private ImageTask imageTask;
    private ImageController controller;
    private Bitmap result;

    private boolean isRemovedOnDetach = false;

    public ImageReceiver() {
        status = STATUS_EMPTY;
    }

    public ImageReceiver(ImageTask imageTask) {
        receiveImage(imageTask);
    }

    public void resetState() {
        status = STATUS_EMPTY;
        result = null;
        imageTask = null;
    }

    public int getStatus() {
        return status;
    }

    public ImageTask getImageTask() {
        return imageTask;
    }

    public void rerequestImage() {
        if (controller != null && imageTask != null && result == null) {
            result = controller.addTask(imageTask);
            if (result != null) {
                status = STATUS_LOADED;
                controller.getReferenceCounter().addReference(imageTask.getKey(), this);
                onImageLoaded(result);
            } else {
                status = STATUS_LOADING;
                onImageLoading();
            }
        } else {
            if (imageTask == null) {
                onNoImage();
            } else {
                if (result == null) {
                    onImageLoading();
                } else {
                    onImageLoaded(result);
                }
            }
        }
    }

    public void receiveImage(ImageTask task) {
        if (imageTask != null && task != null && imageTask.getKey().equals(task.getKey()))
            return;

        if (imageTask != null && controller != null) {
            // controller.getReferenceCounter().removeReference(this);
            controller.removeTask(imageTask);
        }

        isRemovedOnDetach = false;
        imageTask = task;
        result = null;

        if (controller != null && imageTask != null) {
            result = controller.addTask(imageTask);
            if (result != null) {
                status = STATUS_LOADED;
                // controller.getReferenceCounter().addReference(imageTask.getKey(), this);
                onImageLoaded(result);
            } else {
                status = STATUS_LOADING;
                onImageLoading();
            }
        } else {
            if (imageTask == null) {
                status = STATUS_EMPTY;
                onNoImage();
            } else {
                status = STATUS_LOADING;
                onImageLoading();
            }
        }
    }

    public void register(ImageController controller) {
        this.controller = controller;
        controller.addListener(this);
        if (imageTask != null) {
            result = controller.addTask(imageTask);
            if (result != null) {
                status = STATUS_LOADED;
                // controller.getReferenceCounter().addReference(imageTask.getKey(), this);
                onImageLoaded(result);
            }
        }
    }

    public void unregister() {
        controller.removeListener(this);
        if (imageTask != null) {
            controller.removeTask(imageTask);
        }
    }

    public Bitmap getResult() {
        return result;
    }

    public void setResult(Bitmap result) {
        this.result = result;
    }

    @Override
    public void onTaskFinished(ImageTask task) {
        if (imageTask != null) {
            if (task.getKey().equals(imageTask.getKey())) {
                setResult(task.getResult());
                status = STATUS_LOADED;
                // controller.getReferenceCounter().addReference(imageTask.getKey(), this);
                onImageLoaded(getResult());
            }
        }
    }

    @Override
    public void onTaskFailed(ImageTask task) {
        if (imageTask != null) {
            if (task.getKey().equals(imageTask.getKey())) {
                status = STATUS_FAILURE;
                onImageLoadFailure();
            }
        }
    }

    public abstract void onImageLoaded(Bitmap result);

    public abstract void onImageLoadFailure();

    public void onImageTaskEvent(ImageTask task, int eventId, Object... args) {

    }

    public void onImageLoading() {

    }

    public void onNoImage() {

    }

    public void onAddedToParent() {
        if (controller != null) {
            controller.addListener(this);

            if (isRemovedOnDetach) {
                rerequestImage();
                isRemovedOnDetach = false;
            }
        }
    }

    public void onRemovedFromParent() {
        if (controller != null) {
            controller.removeListener(this);
            if (result == null && imageTask != null) {
                isRemovedOnDetach = true;
                controller.removeTask(imageTask);
            }
        }
    }
}
