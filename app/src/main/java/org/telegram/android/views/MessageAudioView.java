package org.telegram.android.views;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.core.Events;
import org.telegram.android.core.audio.AudioPlayerActor;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.preview.AvatarLoader;
import org.telegram.android.preview.ImageHolder;
import org.telegram.android.preview.ImageReceiver;
import org.telegram.android.ui.FontController;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.notifications.StateSubscriber;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by ex3ndr on 12.01.14.
 */
public class MessageAudioView extends MessageBaseDocView implements StateSubscriber {
    private Paint iconBgPaint;
    private Paint progressPaint;
    private Paint progressBgPaint;
    private Paint placeHolderBgPaint;
    private Paint avatarPaint;
    private TextPaint durationPaint;
    private Drawable basePlaceholder;
    private Drawable documentIconOut;
    private Drawable documentIconIn;
    private Drawable documentIconPausedIn;
    private Drawable documentIconPausedOut;
    private Drawable documentIconDownloadIn;
    private Drawable documentIconDownloadOut;

    private Drawable documentIcon;
    private Drawable documentIconDownload;
    private Drawable documentIconPaused;

    private Drawable avatarOverlay;

    private boolean isOut;
    private boolean isInProgress;
    private float progress;
    private String progressText;
    private int durationVal;
    private String duration;

    private User relatedUser;

    private AvatarLoader loader;

    private long avatarAppearTime;
    private ImageHolder contactAvatar;
    private ImageReceiver receiver = new ImageReceiver() {
        @Override
        public void onImageReceived(ImageHolder mediaHolder, boolean intermediate) {
            unbindAvatar();
            contactAvatar = mediaHolder;
            if (intermediate) {
                avatarAppearTime = 0;
            } else {
                avatarAppearTime = SystemClock.uptimeMillis();
            }
            invalidate();
        }
    };

    public MessageAudioView(Context context) {
        super(context);
    }

    public MessageAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageAudioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void unbindAvatar() {
        if (contactAvatar != null) {
            contactAvatar.release();
            contactAvatar = null;
        }
    }

    protected void init() {
        super.init();

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF669dd8);

        progressBgPaint = new Paint();
        progressBgPaint.setStyle(Paint.Style.FILL);
        progressBgPaint.setColor(0xFFc4d2b8);

        documentIconIn = getResources().getDrawable(R.drawable.st_bubble_in_voiceplay);
        documentIconOut = getResources().getDrawable(R.drawable.st_bubble_out_voiceplay);
        documentIconPausedIn = getResources().getDrawable(R.drawable.st_bubble_in_voicepause);
        documentIconPausedOut = getResources().getDrawable(R.drawable.st_bubble_out_voicepause);
        documentIconDownloadIn = getResources().getDrawable(R.drawable.st_bubble_in_voicedownload);
        documentIconDownloadOut = getResources().getDrawable(R.drawable.st_bubble_out_voicedownload);

        avatarOverlay = getResources().getDrawable(R.drawable.st_bubble_voice_gradient);

        iconBgPaint = new Paint();
        iconBgPaint.setStyle(Paint.Style.FILL);
        iconBgPaint.setColor(0xffdff4bd);

        placeHolderBgPaint = new Paint();
        avatarPaint = new Paint();

        durationPaint = new TextPaint();
        durationPaint.setTypeface(FontController.loadTypeface(getContext(), "medium"));
        durationPaint.setTextSize(getSp(12));
        durationPaint.setColor(Color.WHITE);

        loader = application.getUiKernel().getAvatarLoader();
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        super.bindNewView(message);
        bindStateNew(message);
        if (message.message.isOut()) {
            documentIcon = documentIconOut;
            documentIconPaused = documentIconPausedOut;
            documentIconDownload = documentIconDownloadOut;
            progressPaint.setColor(0xff69b449);
            progressBgPaint.setColor(0xffcff1b8);
        } else {
            documentIcon = documentIconIn;
            documentIconPaused = documentIconPausedIn;
            documentIconDownload = documentIconDownloadIn;
            progressPaint.setColor(0xff5498e7);
            progressBgPaint.setColor(0xffe7f0fc);
        }

        this.basePlaceholder = getResources().getDrawable(R.drawable.st_user_placeholder_chat);

        if (message.message.getExtras() instanceof TLLocalAudio) {
            durationVal = ((TLLocalAudio) message.message.getExtras()).getDuration();
            duration = TextUtil.formatDuration(durationVal);
            if (message.forwardUser != null) {
                relatedUser = message.forwardUser;
            } else {
                relatedUser = message.senderUser;
            }
        } else if (message.message.getExtras() instanceof TLUploadingAudio) {
            durationVal = ((TLUploadingAudio) message.message.getExtras()).getDuration();
            duration = TextUtil.formatDuration(durationVal);
            relatedUser = application.getEngine().getUser(application.getCurrentUid());
        } else {
            relatedUser = null;
        }

        unbindAvatar();
        if (relatedUser != null) {
            if (relatedUser.getPhoto() instanceof TLLocalAvatarPhoto) {
                TLLocalAvatarPhoto avatarPhoto = (TLLocalAvatarPhoto) relatedUser.getPhoto();
                loader.requestAvatar(avatarPhoto.getPreviewLocation(), AvatarLoader.TYPE_MEDIUM, receiver);
            } else {
                loader.cancelRequest(receiver);
            }
        } else {
            loader.cancelRequest(receiver);
        }

        if (relatedUser != null) {
            placeHolderBgPaint.setColor(Placeholders.getBgColor(relatedUser.getUid()));
        } else {
            placeHolderBgPaint.setColor(Placeholders.GREY);
        }

        isInProgress = false;
        progress = 0;
        notifications.unregisterSubscriber(this, Events.KIND_AUDIO);
        notifications.registerSubscriber(this, Events.KIND_AUDIO, message.databaseId);
    }

    @Override
    protected void bindUpdate(MessageWireframe message) {
        super.bindUpdate(message);
        bindStateUpdate(message);
    }

    @Override
    public void unbind() {
        super.unbind();
        unbindAvatar();
        notifications.unregisterSubscriber(this, Events.KIND_AUDIO);
    }

    @Override
    protected int measureHeight() {
        return getPx(68);
    }

    @Override
    protected int measureWidth() {
        return relatedUser == null ? super.measureWidth() : (super.measureWidth() + getPx(60));
    }

    public void play() {
        String fileName = application.getDownloadManager().getFileName(getDownloadKey());
        if (isInProgress) {
            notifications.sendState(Events.KIND_AUDIO, databaseId, Events.STATE_PAUSED, progress);
        } else {
            notifications.sendState(Events.KIND_AUDIO, databaseId, Events.STATE_IN_PROGRESS, progress);
        }
        actors.getAudioPlayerActor().talk("toggle", null, (long) databaseId, fileName);
    }

    @Override
    protected void drawContent(Canvas canvas) {

        Drawable icon;
        if (getState() == STATE_DOWNLOADED) {
            icon = isInProgress ? documentIconPaused : documentIcon;
        } else {
            icon = documentIconDownload;
        }

        if (relatedUser != null) {
            rect.set(getPx(6), getPx(6), getPx(62), getPx(62));
            if (contactAvatar != null) {
                avatarPaint.setAlpha(255);
                canvas.drawBitmap(contactAvatar.getBitmap(), new Rect(0, 0, contactAvatar.getW(), contactAvatar.getH()), rect, avatarPaint);
            } else {
                canvas.drawRect(rect, placeHolderBgPaint);
                basePlaceholder.setBounds(rect);
                basePlaceholder.draw(canvas);
            }
            avatarOverlay.setBounds(rect);
            avatarOverlay.draw(canvas);

            if (isInProgress) {
                canvas.drawText(progressText, getPx(6 + 6), getPx(6 + 50), durationPaint);
            } else {
                canvas.drawText(duration, getPx(6 + 6), getPx(6 + 50), durationPaint);
            }

            canvas.save();
            canvas.translate(getPx(56), 0);
        }

        icon.setBounds(new Rect(getPx(22), getPx(22), getPx(22 + 24), getPx(22 + 24)));
        icon.draw(canvas);

        canvas.drawRect(getPx(64), getPx(34), getPx(204), getPx(36), progressBgPaint);

        canvas.drawRect(getPx(64), getPx(34), getPx(64 + (204 - 64) * progress), getPx(36), progressPaint);
        if (relatedUser != null) {
            canvas.restore();
        }
    }

    @Override
    public void onStateChanged(int kind, long id, int state, Object... args) {
        if (state == Events.STATE_IN_PROGRESS) {
            isInProgress = true;
        } else {
            isInProgress = false;
        }
        if (state == Events.STATE_PAUSED || state == Events.STATE_IN_PROGRESS) {
            progress = (Float) args[0];
            progressText = TextUtil.formatDuration((int) (progress * durationVal));
        } else {
            progress = 0;
            progressText = TextUtil.formatDuration(0);
        }
        invalidate();
    }
}
