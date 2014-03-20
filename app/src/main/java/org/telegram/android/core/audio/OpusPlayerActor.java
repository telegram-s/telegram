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

    public OpusPlayerActor(ActorSystem system) {
        super(system, Actors.THREAD_AUDIO);
        opusLib = new OpusLib();
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            if (state != STATE_NONE) {
                destroyPlayer();
            }
            opusLib.openOpusFile(((PlayAudio) message).fileName);

            bufferSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            state = STATE_STARTED;
            sendMessage(new IterateAudio());
        } else if (message instanceof IterateAudio) {
            if (state == STATE_NONE) {
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
            if (!isFinished) {
                sendMessage(new IterateAudio());
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

    public static class ToggleAudio extends Message {

    }

    // Internal

    public static class IterateAudio extends Message {

    }
}
