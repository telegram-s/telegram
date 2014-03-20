package org.telegram.android.core.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import org.telegram.android.actors.Actors;
import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class OpusPlayerActor extends Actor<OpusPlayerActor.Message> {

    private OpusLib opusLib;

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_PAUSED = 2;

    private int state = STATE_NONE;
    private AudioTrack audioTrack;
    private int bufferSize;
    private long duration;
    private long offset;

    private long currentId;
    private String currentFileName;
    private AudioPlayerActor basePlayer;

    public OpusPlayerActor(AudioPlayerActor audioPlayerActor, ActorSystem system) {
        super(system, Actors.THREAD_AUDIO);
        this.basePlayer = audioPlayerActor;
        this.opusLib = new OpusLib();
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            if (state != STATE_NONE) {
                destroyPlayer();
            }
            state = STATE_NONE;
            currentFileName = ((PlayAudio) message).fileName;
            currentId = ((PlayAudio) message).id;

            int res = opusLib.openOpusFile(currentFileName);
            if (res == 0) {
                basePlayer.sendMessage(new AudioPlayerActor.SubPlayerCrash(currentId));
                return;
            }

            duration = opusLib.getTotalPcmDuration();
            offset = 0;

            try {
                bufferSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                audioTrack.play();
            } catch (Exception e) {
                e.printStackTrace();
                destroyPlayer();
                basePlayer.sendMessage(new AudioPlayerActor.SubPlayerCrash(currentId));
                return;
            }

            state = STATE_STARTED;
            sendMessage(new IterateAudio());
        } else if (message instanceof IterateAudio) {
            if (state != STATE_STARTED) {
                return;
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            opusLib.readOpusFile(buffer, bufferSize);
            int size = opusLib.getSize();
            long pmcOffset = opusLib.getPcmOffset();
            boolean isFinished = opusLib.getFinished() == 1;
            if (size != 0) {
                buffer.rewind();
                byte[] data = new byte[size];
                buffer.get(data);
                audioTrack.write(data, 0, size);
            }
            offset = pmcOffset;
            float scale = 0;
            if (duration != 0) {
                scale = offset / (float) duration;
            }
            basePlayer.sendMessage(new AudioPlayerActor.SubPlayerInProgress(currentId, scale));
            if (!isFinished) {
                sendMessage(new IterateAudio());
            } else {
                sendMessage(new StopAudio());
            }
        } else if (message instanceof PauseAudio) {
            if (state == STATE_STARTED) {
                audioTrack.pause();
                state = STATE_PAUSED;
            }
        } else if (message instanceof ResumeAudio) {
            if (state == STATE_PAUSED) {
                audioTrack.play();
                sendMessage(new IterateAudio());
                state = STATE_STARTED;
            }
        } else if (message instanceof StopAudio) {
            destroyPlayer();
            state = STATE_NONE;
            basePlayer.sendMessage(new AudioPlayerActor.SubPlayerStop(currentId));
        } else if (message instanceof ToggleAudio) {
            if (state == STATE_PAUSED) {
                sendMessage(new ResumeAudio());
            } else if (state == STATE_STARTED) {
                sendMessage(new PauseAudio());
            } else {
                sendMessage(new PlayAudio(currentId, currentFileName));
            }
        }
    }

    private void destroyPlayer() {
        opusLib.closeOpusFile();
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }

    public static abstract class Message {

    }

    public static class PlayAudio extends Message {
        private String fileName;
        private long id;

        public PlayAudio(long id, String fileName) {
            this.id = id;
            this.fileName = fileName;
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

    public static class ResumeAudio extends Message {

    }

    public static class StopAudio extends Message {

    }

    public static class ToggleAudio extends Message {

    }

    // Internal

    public static class IterateAudio extends Message {

    }
}
