package org.telegram.android.core.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import org.telegram.android.TelegramApplication;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.io.File;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AndroidPlayerActor extends Actor<AndroidPlayerActor.Message> {

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_PAUSED = 2;

    private int state = STATE_NONE;

    private MediaPlayer mplayer;
    private TelegramApplication application;

    public AndroidPlayerActor(TelegramApplication application, ActorSystem system) {
        super(system, "common");
        this.application = application;
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            destroyPlayer();

            mplayer = new MediaPlayer();
            mplayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mplayer.setDataSource(application, Uri.fromFile(new File(((PlayAudio) message).fileName)));
            mplayer.prepare();
            mplayer.setLooping(false);
            mplayer.start();
            state = STATE_STARTED;
        } else if (message instanceof PauseAudio) {
            if (state == STATE_STARTED) {
                mplayer.pause();
                state = STATE_PAUSED;
            }
        } else if (message instanceof ResumeAudio) {
            if (state == STATE_PAUSED) {
                mplayer.start();
                state = STATE_STARTED;
            }
        } else if (message instanceof StopAudio) {
            destroyPlayer();
        }
    }

    private void destroyPlayer() {
        if (mplayer != null) {
            mplayer.stop();
            mplayer.reset();
            mplayer.release();
            mplayer = null;
        }
    }

    public static abstract class Message {

    }

    public static class PlayAudio extends Message {
        private String fileName;

        public PlayAudio(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class PauseAudio extends Message {

    }

    public static class ResumeAudio extends Message {

    }

    public static class StopAudio extends Message {

    }
}
