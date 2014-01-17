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
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLUploadingDocument;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.media.DownloadManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by ex3ndr on 12.01.14.
 */
public class MessageAudioView extends MessageBaseDocView {

    private static MediaPlayer mediaPlayer;
    private static int lastDatabaseId;
    private static WeakReference<MessageAudioView> lastView;
    private static Executor audioLoader = Executors.newSingleThreadExecutor();

    private Paint iconBgPaint;
    private Paint progressPaint;
    private Paint progressBgPaint;
    private Drawable documentIconOut;
    private Drawable documentIconPausedOut;
    private Drawable documentIconIn;
    private Drawable documentIconPausedIn;
    private Drawable documentIcon;
    private Drawable documentIconPaused;

    private boolean isDocument;

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
        if (message.message.isOut()) {
            iconBgPaint.setColor(0xffdef3bd);
            documentIcon = documentIconOut;
            documentIconPaused = documentIconPausedOut;
        } else {
            iconBgPaint.setColor(0xfff1f4f6);
            documentIcon = documentIconIn;
            documentIconPaused = documentIconPausedIn;
        }
        isDocument = message.message.getExtras() instanceof TLUploadingDocument
                || message.message.getExtras() instanceof TLLocalDocument;
    }

    public void play() {
        if (lastDatabaseId != databaseId) {
            lastDatabaseId = databaseId;

            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;

                if (lastView != null) {
                    MessageAudioView view = lastView.get();
                    if (view != null) {
                        view.postInvalidate();
                    }
                }
            }

            lastView = new WeakReference<MessageAudioView>(this);

            audioLoader.execute(new Runnable() {
                @Override
                public void run() {
                    if (lastDatabaseId != databaseId) {
                        return;
                    }
                    try {
                        MediaPlayer mplayer = new MediaPlayer();
                        mplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mplayer.setDataSource(application, Uri.fromFile(
                                new File(isDocument ? application.getDownloadManager().getDocFileName(key) :
                                        application.getDownloadManager().getAudioFileName(key))));
                        mplayer.prepare();
                        mplayer.setLooping(false);
                        mplayer.start();
                        mplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                if (lastDatabaseId != databaseId) {
                                    return;
                                }
                                mediaPlayer.stop();
                                mediaPlayer.reset();
                                mediaPlayer.release();
                                mediaPlayer = null;
                                lastDatabaseId = 0;
                                if (lastView != null) {
                                    MessageAudioView view = lastView.get();
                                    if (view != null) {
                                        view.postInvalidate();
                                    }
                                    lastView = null;
                                }
                                postInvalidate();
                            }
                        });

                        if (lastDatabaseId != databaseId) {
                            return;
                        }

                        mediaPlayer = mplayer;
                        postInvalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                lastView = null;
                mediaPlayer = null;
            }
            lastDatabaseId = 0;
            invalidate();
        }
    }

    @Override
    protected void drawContent(Canvas canvas) {
        canvas.drawRect(new Rect(getPx(4), getPx(4), getPx(4 + 48), getPx(4 + 48)), iconBgPaint);

        Drawable icon;
        if (lastDatabaseId == databaseId) {
            icon = documentIconPaused;
        } else {
            icon = documentIcon;
        }

        icon.setBounds(new Rect(getPx(12), getPx(12), getPx(12 + 32), getPx(12 + 32)));
        icon.draw(canvas);

        canvas.drawRect(getPx(64), getPx(28), getPx(204), getPx(30), progressBgPaint);

        if (mediaPlayer != null && lastDatabaseId == databaseId) {
            int duration = mediaPlayer.getDuration();
            int progress = mediaPlayer.getCurrentPosition();

            if (duration != 0) {
                canvas.drawRect(getPx(64), getPx(28), getPx(64 + (204 - 64) * progress / duration), getPx(30), progressPaint);
            }

            postInvalidateDelayed(100);
        }
    }
}
