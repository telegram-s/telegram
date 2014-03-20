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
    private AudioPlayerActor basePlayer;

    private long currentId;
    private String currentFileName;

    public AndroidPlayerActor(AudioPlayerActor basePlayer, TelegramApplication application, ActorSystem system) {
        super(system, "common");
        this.application = application;
        this.basePlayer = basePlayer;
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            currentId = ((PlayAudio) message).id;
            currentFileName = ((PlayAudio) message).fileName;

            destroyPlayer();

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
                        sendMessage(new StopAudio());
                    }
                });
                mplayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        sendMessage(new ErrorAudio());
                        return false;
                    }
                });
            } catch (Exception e) {
                destroyPlayer();
                basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerCrash(currentId));
                return;
            }

            basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerStart(currentId));
            sendMessage(new NotifyAudio(), 500);
            state = STATE_STARTED;
        } else if (message instanceof NotifyAudio) {
            if (mplayer != null) {
                if (state == STATE_STARTED) {
                    int duration = mplayer.getDuration();
                    if (duration == 0) {
                        basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerInProgress(currentId, 0));
                    } else {
                        float progress = ((float) mplayer.getCurrentPosition()) / duration;
                        basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerInProgress(currentId, progress));
                    }
                    sendMessage(new NotifyAudio(), 500);
                }
            }
        } else if (message instanceof PauseAudio) {
            if (mplayer != null) {
                if (mplayer.isPlaying()) {
                    mplayer.pause();
                }
                state = STATE_PAUSED;
            }
        } else if (message instanceof ResumeAudio) {
            if (mplayer != null) {
                if (!mplayer.isPlaying()) {
                    mplayer.start();
                }
                state = STATE_STARTED;
            }
        } else if (message instanceof StopAudio) {
            destroyPlayer();
            basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerStop(currentId));
        } else if (message instanceof ToggleAudio) {
            if (state == STATE_PAUSED) {
                sendMessage(new ResumeAudio());
            } else if (state == STATE_STARTED) {
                sendMessage(new PauseAudio());
            } else {
                sendMessage(new PlayAudio(currentId, currentFileName));
            }
        } else if (message instanceof RestartAudio) {
            sendMessage(new PlayAudio(currentId, currentFileName));
        } else if (message instanceof ErrorAudio) {
            destroyPlayer();
            basePlayer.sendMessage(new AudioPlayerActor.AndroidPlayerCrash(currentId));
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
        private long id;

        public PlayAudio(long id, String fileName) {
            this.fileName = fileName;
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class PauseAudio extends Message {

    }

    public static class RestartAudio extends Message {

    }

    public static class ResumeAudio extends Message {

    }

    public static class StopAudio extends Message {

    }

    public static class ToggleAudio extends Message {

    }

    public static class ErrorAudio extends Message {

    }

    public static class NotifyAudio extends Message {

    }
}
