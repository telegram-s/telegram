package org.telegram.android.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class AvatarView extends View implements ImageReceiver {
    private Drawable emptyDrawable;
    private ImageHolder holder;

    private long arriveTime;
    private TelegramApplication application;
    private AvatarLoader loader;
    private Rect rect = new Rect();
    private Rect rect1 = new Rect();
    private Paint avatarPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private boolean isBinded = false;
    private TLAbsLocalFileLocation fileLocation;
    private String fileName;
    private Uri fileUri;

    public AvatarView(Context context) {
        super(context);
        application = (TelegramApplication) context.getApplicationContext();
        loader = application.getUiKernel().getAvatarLoader();
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        application = (TelegramApplication) context.getApplicationContext();
        loader = application.getUiKernel().getAvatarLoader();
    }

    public AvatarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        application = (TelegramApplication) context.getApplicationContext();
        loader = application.getUiKernel().getAvatarLoader();
    }

    public void setEmptyGreyUser() {

    }

    public void setEmptyGreyGroup() {

    }

    public void setEmptyUser(String title, int index) {

    }

    public void setEmptyUser(int index) {

    }

    public void setEmptyGroup(int index) {

    }

    public void setEmptyGroup(String title, int index) {

    }

    public Drawable getEmptyDrawable() {
        return emptyDrawable;
    }

    public void setEmptyDrawable(Drawable emptyDrawable) {
        this.emptyDrawable = emptyDrawable;
    }

    public void setEmptyDrawable(int res) {
        this.emptyDrawable = getResources().getDrawable(res);
    }

    public void requestAvatar(TLAbsLocalFileLocation fileLocation) {
        unbindAvatar();
        this.fileLocation = fileLocation;
        this.fileName = null;
        this.fileUri = null;
        bindLocation();
    }

    public void requestRawAvatar(String fileName) {
        unbindAvatar();
        this.fileLocation = null;
        this.fileName = fileName;
        this.fileUri = null;
        bindLocation();
    }

    public void requestRawAvatar(Uri uri) {
        unbindAvatar();
        this.fileLocation = null;
        this.fileName = null;
        this.fileUri = uri;
        bindLocation();
    }

    public void removeAvatar() {
        requestAvatar(null);
    }

    private void bindLocation() {
        if (fileLocation != null) {
            if (!loader.requestAvatar(fileLocation, AvatarLoader.TYPE_MEDIUM, this)) {
                unbindAvatar();
            }
            isBinded = true;
        } else if (fileName != null) {
            if (!loader.requestRawAvatar(fileName, this)) {
                unbindAvatar();
            }
            isBinded = true;
        } else if (fileUri != null) {
            if (!loader.requestRawAvatar(fileUri, this)) {
                unbindAvatar();
            }
            isBinded = true;
        } else {
            if (isBinded) {
                unbindAvatar();
                loader.cancelRequest(this);
                isBinded = false;
            }
        }
        invalidate();
    }

    private void unbindAvatar() {
        if (holder != null) {
            holder.release();
            holder = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isBinded) {
            bindLocation();
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbindAvatar();
        if (isBinded) {
            loader.cancelRequest(this);
            isBinded = false;
        }
        invalidate();
    }

    public void requestAvatarSwitch(TLAbsLocalFileLocation fileLocation) {
        requestAvatar(fileLocation);
    }

    public void requestRawAvatarSwitch(String fileName) {
        requestRawAvatar(fileName);
    }

    public void requestRawAvatarSwitch(Uri uri) {
        requestRawAvatar(uri);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (emptyDrawable != null) {
            emptyDrawable.setBounds(0, 0, getWidth(), getHeight());
            emptyDrawable.draw(canvas);
        }
        if (holder != null) {
            rect.set(0, 0, holder.getBitmap().getWidth(), holder.getBitmap().getHeight());
            rect1.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(holder.getBitmap(), rect, rect1, avatarPaint);
        }
    }


    @Override
    public void onImageReceived(ImageHolder avatarHolder, boolean intermediate) {
        unbindAvatar();
        holder = avatarHolder;
        if (intermediate) {
            this.arriveTime = 0;
        } else {
            this.arriveTime = SystemClock.uptimeMillis();
        }
        invalidate();
    }
}
