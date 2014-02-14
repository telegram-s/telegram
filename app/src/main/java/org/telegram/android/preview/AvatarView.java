package org.telegram.android.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;

/**
 * Created by ex3ndr on 06.02.14.
 */
public class AvatarView extends View implements AvatarReceiver {

    private Drawable emptyDrawable;
    private Bitmap avatar;
    private String avatarKey;

    private long arriveTime;
    private TelegramApplication application;
    private Rect rect = new Rect();
    private Rect rect1 = new Rect();
    private Paint avatarPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private TLAbsLocalFileLocation fileLocation;

    public AvatarView(Context context) {
        super(context);
        application = (TelegramApplication) context.getApplicationContext();
    }

    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        application = (TelegramApplication) context.getApplicationContext();
    }

    public AvatarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        application = (TelegramApplication) context.getApplicationContext();
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
        bindLocation();
    }

    private void bindLocation() {
        if (fileLocation == null) {
            this.application.getUiKernel().getAvatarLoader().cancelRequest(null);
        } else {
            this.application.getUiKernel().getAvatarLoader().requestAvatar(fileLocation, AvatarLoader.TYPE_MEDIUM, this);
        }
        invalidate();
    }

    private void unbindAvatar() {
        if (avatar != null) {
            application.getUiKernel().getAvatarLoader().getImageCache().decReference(avatarKey, this);
            avatar = null;
            avatarKey = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        bindLocation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unbindAvatar();
    }

    public void requestAvatarSwitch(TLAbsLocalFileLocation fileLocation) {
        requestAvatar(fileLocation);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (emptyDrawable != null) {
            emptyDrawable.setBounds(0, 0, getWidth(), getHeight());
            emptyDrawable.draw(canvas);
        }
        if (avatar != null) {
            rect.set(0, 0, avatar.getWidth(), avatar.getHeight());
            rect1.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(avatar, rect, rect1, avatarPaint);
        }
    }

    @Override
    public void onAvatarReceived(Bitmap original, String key, boolean intermediate) {
        unbindAvatar();
        this.avatar = original;
        this.avatarKey = key;
        application.getUiKernel().getAvatarLoader().getImageCache().incReference(key, this);
        if (intermediate) {
            this.arriveTime = 0;
        } else {
            this.arriveTime = SystemClock.uptimeMillis();
        }
        invalidate();
    }

    @Override
    public String toString() {
        return "AvatarView#" + hashCode();
    }
}
