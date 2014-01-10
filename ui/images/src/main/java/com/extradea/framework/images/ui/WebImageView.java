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

package com.extradea.framework.images.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.extradea.framework.images.ImageReceiver;
import com.extradea.framework.images.ReceiverControllerCallback;
import com.extradea.framework.images.ImageSupport;
import com.extradea.framework.images.ReceiverController;
import com.extradea.framework.images.tasks.ImageDownloadTask;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.tasks.RoundedImageTask;

/**
 * User: Stepan (aka Ex3NDR) Korshakov Date: 28.03.12 Time: 17:45
 */
public class WebImageView extends ImageView implements ReceiverControllerCallback {

    private ImageSupport imageSupport;
    private ReceiverController receiverController;

    private Drawable errorImage;
    private Drawable nothingImage;

    private boolean repeatForever = false;

    private ImageReceiver receiver;

    private boolean fitXY = false;
    private ScaleType originalScaleType;

    public WebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public WebImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WebImageView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof ImageSupport) {
            imageSupport = (ImageSupport) context;
        } else if (context.getApplicationContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) context.getApplicationContext();
        }


        if (context instanceof ReceiverController) {
            receiverController = (ReceiverController) context;
        } else if (context.getApplicationContext() instanceof ReceiverController) {
            receiverController = (ReceiverController) context.getApplicationContext();
        }

        nothingImage = null;
        errorImage = null;
        originalScaleType = getScaleType();

        receiver = new ImageReceiver() {
            @Override
            public void onImageLoaded(final Bitmap result) {
                smartPost(new Runnable() {
                    @Override
                    public void run() {
                        setImageBitmap(result);
                        setScaleTypeInternal(originalScaleType);
                    }
                });
            }

            @Override
            public void onImageLoadFailure() {
                smartPost(new Runnable() {
                    @Override
                    public void run() {
                        setImageDrawable(errorImage);
                        if (fitXY) {
                            setScaleTypeInternal(ScaleType.FIT_XY);
                        }
                        if (repeatForever) {
                            receiver.rerequestImage();
                        }
                    }
                });
            }

            @Override
            public void onImageLoading() {
                smartPost(new Runnable() {
                    @Override
                    public void run() {
                        if (receiver.getStatus() != ImageReceiver.STATUS_LOADED) {
                            setWaitingState();
                        }
                    }
                });
            }

            @Override
            public void onNoImage() {
            }
        };

        if (receiverController != null) {
            receiverController.registerRecieverView(receiver);
        }

        if (imageSupport != null) {
            receiver.register(imageSupport.getImageController());
        }
    }

    private void smartPost(Runnable runnable) {
        if (Looper.getMainLooper() == null)
            return;

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            post(runnable);
        }
    }


    private void setScaleTypeInternal(ScaleType scaleType) {
        super.setScaleType(scaleType);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (!fitXY) {
            setScaleTypeInternal(scaleType);
        }
        originalScaleType = scaleType;
    }

    private void setWaitingState() {
        setImageDrawable(nothingImage);
        if (fitXY) {
            setScaleTypeInternal(ScaleType.FIT_XY);
        }

        smartPost(new Runnable() {
            @Override
            public void run() {
                if (getDrawable() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = ((AnimationDrawable) getDrawable());
                    animationDrawable.setCallback(WebImageView.this);
                    animationDrawable.setVisible(true, true);
                    if (!animationDrawable.isRunning()) {
                        animationDrawable.stop();
                        animationDrawable.start();
                    }
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        smartPost(new Runnable() {
            @Override
            public void run() {
                if (getDrawable() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = ((AnimationDrawable) getDrawable());
                    animationDrawable.setCallback(WebImageView.this);
                    animationDrawable.setVisible(true, true);
                    if (!animationDrawable.isRunning()) {
                        animationDrawable.stop();
                        animationDrawable.start();
                    }
                }
            }
        });
    }

    public boolean isRepeatForever() {
        return repeatForever;
    }


    public void setRepeatForever(boolean repeatForever) {
        this.repeatForever = repeatForever;
    }

    public void setStatusImage(Drawable drawable) {
        setErrorImage(drawable);
        setWaitingImage(drawable);
    }

    public void setErrorImage(Drawable errorImage) {
        this.errorImage = errorImage;
    }

    public Drawable getErrorImage() {
        return errorImage;
    }

    public void setWaitingImage(Drawable nothingImage) {
        this.nothingImage = nothingImage;
    }

    public Drawable getWaitingImage() {
        return nothingImage;
    }

    public void setImageTask(ImageTask img) {
        receiver.receiveImage(img);
        smartPost(new Runnable() {
            @Override
            public void run() {
                Bitmap result = receiver.getResult();
                if (result == null) {
                    setWaitingState();
                }
            }
        });
    }

    public void setRoundedUrl(String url, int r) {
        if (url != null) {
            RoundedImageTask task = new RoundedImageTask(url);
            task.setRadius((int) (getResources().getDisplayMetrics().density * r));
            setImageTask(task);
        } else {
            setImageTask(null);
        }
    }

    public void setRoundedUrl(String url) {
        if (url != null) {
            setImageTask(new RoundedImageTask(url));
        } else {
            setImageTask(null);
        }
    }

    public void setUrl(final String url, boolean isMemoryCached) {
        if (url != null) {
            ImageDownloadTask task = new ImageDownloadTask(url);
            task.setPutInMemoryCache(isMemoryCached);
            setImageTask(task);
        } else {
            setImageTask(null);
        }
    }

    public void setUrl(final String url) {
        setUrl(url, true);
    }

    public ImageReceiver getReceiver() {
        return receiver;
    }

    public void recycleImage() {
        if (receiver.getResult() != null) {
            receiver.getResult().recycle();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        receiver.resetState();
        setImageBitmap(null);
        setWaitingState();
    }

    public void onRemovedFromParent() {
        receiver.onRemovedFromParent();
    }

    public void onAddedToParent() {
        receiver.onAddedToParent();
    }

    public void enableFitXYForProcessingStates() {
        fitXY = true;
    }
}