package org.telegram.android.core.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import org.telegram.android.TelegramApplication;
import org.telegram.threading.ActorReference;
import org.telegram.threading.ActorSystem;
import org.telegram.threading.ReflectedActor;

import java.io.File;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AndroidPlayerActor extends ReflectedActor {

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_PAUSED = 2;

    private int state = STATE_NONE;

    private MediaPlayer mplayer;
    private TelegramApplication application;
    private AudioPlayerActor.SubMessenger basePlayer;

    private long currentId;
    private String currentFileName;

    public AndroidPlayerActor(ActorReference basePlayer, TelegramApplication application, ActorSystem system) {
        super(system, "common");
        this.application = application;
        this.basePlayer = new AudioPlayerActor.SubMessenger(basePlayer, self());
    }

    protected void onPlayMessage(long id, String fileName) throws Exception {
        currentId = id;
        currentFileName = fileName;

        destroyPlayer();
        state = STATE_NONE;

        try {
            mplayer = new MediaPlayer();
            mplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mplayer.setDataSource(application, Uri.fromFile(new File(currentFileName)));
            mplayer.prepare();
            mplayer.setLooping(false);
            mplayer.start();
            mplayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    self().talk("stop", self());
                }
            });
            mplayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    self().talk("error", self());
                    return false;
                }
            });
        } catch (Exception e) {
            destroyPlayer();
            basePlayer.crash(currentId);
            return;
        }

        basePlayer.started(currentId);
        self().talkDelayed("notify", self(), 500);
        state = STATE_STARTED;
    }

    protected void onNotifyMessage() throws Exception {
        if (mplayer != null) {
            if (state == STATE_STARTED) {
                int duration = mplayer.getDuration();
                if (duration == 0) {
                    basePlayer.progress(currentId, 0);
                } else {
                    float progress = ((float) mplayer.getCurrentPosition()) / duration;
                    basePlayer.progress(currentId, progress);
                }
                self().talkDelayed("notify", self(), 500);
            }
        }
    }

    protected void onPauseMessage() throws Exception {
        if (mplayer != null) {
            if (mplayer.isPlaying()) {
                mplayer.pause();
            }
            state = STATE_PAUSED;
        }
    }

    protected void onResumeMessage() throws Exception {
        if (mplayer != null) {
            if (!mplayer.isPlaying()) {
                mplayer.start();
            }
            state = STATE_STARTED;
        }
    }

    protected void onStopMessage() throws Exception {
        destroyPlayer();
        basePlayer.started(currentId);
    }

    protected void onToggleMessage(long id, String fileName) throws Exception {
        if (state == STATE_PAUSED) {
            onResumeMessage();
        } else if (state == STATE_STARTED) {
            onPauseMessage();
        } else {
            onPlayMessage(id, fileName);
        }
    }

    protected void onRestartMessage() throws Exception {
        onPlayMessage(currentId, currentFileName);
    }

    protected void onErrorMessage() throws Exception {
        destroyPlayer();
        basePlayer.crash(currentId);
    }

    @Override
    public void onException(Exception e) {
        e.printStackTrace();
        destroyPlayer();
        basePlayer.crash(currentId);
    }

    private void destroyPlayer() {
        if (mplayer != null) {
            mplayer.stop();
            mplayer.reset();
            mplayer.release();
            mplayer = null;
        }
    }
}
