package org.telegram.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import org.telegram.android.R;
import org.telegram.android.core.Events;
import org.telegram.android.core.audio.AudioPlayerActor;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLUploadingDocument;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.DownloadManager;
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
    private Drawable documentIconOut;
    private Drawable documentIconPausedOut;
    private Drawable documentIconIn;
    private Drawable documentIconPausedIn;
    private Drawable documentIcon;
    private Drawable documentIconPaused;

    private boolean isInProgress;
    private float progress;

    public MessageAudioView(Context context) {
        super(context);
    }

    public MessageAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageAudioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void init() {
        super.init();

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(0xFF669dd8);

        progressBgPaint = new Paint();
        progressBgPaint.setStyle(Paint.Style.FILL);
        progressBgPaint.setColor(0xFFacd0f7);

        documentIconOut = getResources().getDrawable(R.drawable.st_bubble_ic_play);
        documentIconIn = getResources().getDrawable(R.drawable.st_bubble_ic_play);
        documentIconPausedOut = getResources().getDrawable(R.drawable.st_bubble_ic_pause);
        documentIconPausedIn = getResources().getDrawable(R.drawable.st_bubble_ic_pause);

        iconBgPaint = new Paint();
        iconBgPaint.setStyle(Paint.Style.FILL);
        iconBgPaint.setColor(0xffdff4bd);
    }

    @Override
    protected void bindNewView(MessageWireframe message) {
        super.bindNewView(message);
        bindStateNew(message);
        if (message.message.isOut()) {
            iconBgPaint.setColor(0xffdef3bd);
            documentIcon = documentIconOut;
            documentIconPaused = documentIconPausedOut;
        } else {
            iconBgPaint.setColor(0xfff1f4f6);
            documentIcon = documentIconIn;
            documentIconPaused = documentIconPausedIn;
        }

        isInProgress = false;
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
        notifications.unregisterSubscriber(this, Events.KIND_AUDIO);
    }

    public void play() {
        String fileName = application.getDownloadManager().getFileName(getDownloadKey());
        if (isInProgress) {
            notifications.sendState(Events.KIND_AUDIO, databaseId, Events.STATE_PAUSED, progress);
        } else {
            notifications.sendState(Events.KIND_AUDIO, databaseId, Events.STATE_IN_PROGRESS, progress);
        }
        actors.getAudioPlayerActor().sendMessage(new AudioPlayerActor.ToggleAudio(databaseId, fileName));
    }

    @Override
    protected void drawContent(Canvas canvas) {
        canvas.drawRect(new Rect(getPx(4), getPx(4), getPx(4 + 48), getPx(4 + 48)), iconBgPaint);

        Drawable icon = isInProgress ? documentIconPaused : documentIcon;

        icon.setBounds(new Rect(getPx(12), getPx(12), getPx(12 + 32), getPx(12 + 32)));
        icon.draw(canvas);

        canvas.drawRect(getPx(64), getPx(28), getPx(204), getPx(30), progressBgPaint);

        canvas.drawRect(getPx(64), getPx(28), getPx(64 + (204 - 64) * progress), getPx(30), progressPaint);

//        if (mediaPlayer != null && lastDatabaseId == databaseId) {
//            int duration = mediaPlayer.getDuration();
//            int progress = mediaPlayer.getCurrentPosition();
//
//            if (duration != 0) {
//                canvas.drawRect(getPx(64), getPx(28), getPx(64 + (204 - 64) * progress / duration), getPx(30), progressPaint);
//            }
//
//            postInvalidateDelayed(100);
//        }
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
        } else {
            progress = 0;
        }
        invalidate();
    }
}
