package com.extradea.framework.images.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.extradea.framework.images.ImageReceiver;
import com.extradea.framework.images.ImageSupport;
import com.extradea.framework.images.ReceiverController;
import com.extradea.framework.images.tasks.ImageTask;

/**
 * Author: Korshakov Stepan
 * Created: 02.06.13 19:17
 */
public class FastWebImageView extends View {

    public static final int SCALE_TYPE_FILL = 0;
    public static final int SCALE_TYPE_FIT = 1;
    public static final int SCALE_TYPE_FIT_CROP = 2;

    private static final int FADE_OUT_ANIMATION_TIME = 300;

    private ImageSupport imageSupport;

    private Drawable currentDrawable;
    private Drawable prevDrawable;
    private Drawable emptyDrawable;

    private ImageReceiver receiver;

    private long showingTime = 0;

    private int scaleTypeImage = SCALE_TYPE_FILL;
    private int scaleTypeEmpty = SCALE_TYPE_FILL;

    private boolean isFadeEnabled = true;

    public FastWebImageView(Context context) {
        super(context);
        init(context);
    }

    public FastWebImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FastWebImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof ImageSupport) {
            imageSupport = (ImageSupport) context;
        } else if (context.getApplicationContext() instanceof ImageSupport) {
            imageSupport = (ImageSupport) context.getApplicationContext();
        }

        receiver = new ImageReceiver() {
            @Override
            public void onImageLoaded(final Bitmap result) {
                currentDrawable = new BitmapDrawable(result);
                showingTime = android.os.SystemClock.uptimeMillis();
                postInvalidate();
            }

            @Override
            public void onImageLoadFailure() {
            }

            @Override
            public void onImageLoading() {
                currentDrawable = null;
                postInvalidate();
            }

            @Override
            public void onNoImage() {
                currentDrawable = null;
                postInvalidate();
            }
        };

        if (imageSupport != null) {
            receiver.register(imageSupport.getImageController());
        }
    }

    public void requestTask(ImageTask task) {
        prevDrawable = null;
        receiver.receiveImage(task);
        if (currentDrawable != null) {
            showingTime = 0;
        }
    }

    public void requestTaskSwitch(ImageTask task) {
        if (currentDrawable == null && prevDrawable == null) {
            prevDrawable = emptyDrawable;
        } else if (currentDrawable != null) {
            prevDrawable = currentDrawable;
        }
        receiver.receiveImage(task);
        if (currentDrawable != null) {
            showingTime = 0;
        }
    }

    public void setLoadingDrawable(Drawable drawable) {
        emptyDrawable = drawable;
    }

    public void setLoadingDrawable(int res) {
        emptyDrawable = getResources().getDrawable(res);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        receiver.onRemovedFromParent();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        receiver.onAddedToParent();
    }

    public boolean isFadeEnabled() {
        return isFadeEnabled;
    }

    public void setFadeEnabled(boolean fadeEnabled) {
        isFadeEnabled = fadeEnabled;
    }

    public int getScaleTypeImage() {
        return scaleTypeImage;
    }

    public void setScaleTypeImage(int scaleTypeImage) {
        this.scaleTypeImage = scaleTypeImage;
    }

    public int getScaleTypeEmpty() {
        return scaleTypeEmpty;
    }

    public void setScaleTypeEmpty(int scaleTypeEmpty) {
        this.scaleTypeEmpty = scaleTypeEmpty;
    }

    private void setBounds(Drawable src, int scaleType) {
        switch (scaleType) {
            default:
            case SCALE_TYPE_FILL:
                src.setBounds(0, 0, getWidth(), getHeight());
                break;
            case SCALE_TYPE_FIT:
                float scaleFactor = Math.min(getWidth() / (float) src.getIntrinsicWidth(),
                        getHeight() / (float) src.getIntrinsicHeight());
                int width = (int) (scaleFactor * src.getIntrinsicWidth());
                int height = (int) (scaleFactor * src.getIntrinsicHeight());
                int deltaX = (getWidth() - width) / 2;
                int deltaY = (getHeight() - height) / 2;
                src.setBounds(deltaX, deltaY, deltaX + width, deltaY + height);
                break;
            case SCALE_TYPE_FIT_CROP:
                float scaleFactor2 = Math.max(getWidth() / (float) src.getIntrinsicWidth(),
                        getHeight() / (float) src.getIntrinsicHeight());
                int width2 = (int) (scaleFactor2 * src.getIntrinsicWidth());
                int height2 = (int) (scaleFactor2 * src.getIntrinsicHeight());
                int deltaX2 = (getWidth() - width2) / 2;
                int deltaY2 = (getHeight() - height2) / 2;
                src.setBounds(deltaX2, deltaY2, deltaX2 + width2, deltaY2 + height2);
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentDrawable != null) {
            long time = android.os.SystemClock.uptimeMillis();
            if (time - showingTime > FADE_OUT_ANIMATION_TIME || !isFadeEnabled) {
                currentDrawable.setAlpha(255);
                setBounds(currentDrawable, scaleTypeImage);
                currentDrawable.draw(canvas);
            } else {
                float mainAlpha = (time - showingTime) / (float) FADE_OUT_ANIMATION_TIME;

                if (prevDrawable != null) {
                    prevDrawable.setAlpha(255);
                    setBounds(prevDrawable, scaleTypeImage);
                    prevDrawable.draw(canvas);
                } else if (emptyDrawable != null) {
                    emptyDrawable.setAlpha(255);
                    setBounds(emptyDrawable, scaleTypeEmpty);
                    emptyDrawable.draw(canvas);
                }

                currentDrawable.setAlpha((int) (255 * mainAlpha));
                setBounds(currentDrawable, scaleTypeImage);
                currentDrawable.draw(canvas);
                currentDrawable.setAlpha(255);

                invalidate();
            }
        } else if (prevDrawable != null) {
            prevDrawable.setAlpha(255);
            setBounds(prevDrawable, scaleTypeImage);
            prevDrawable.draw(canvas);
        } else if (emptyDrawable != null) {
            emptyDrawable.setAlpha(255);
            setBounds(emptyDrawable, scaleTypeEmpty);
            emptyDrawable.draw(canvas);
        }

    }
}
