package org.telegram.android.core.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import org.telegram.android.actors.Actors;
import org.telegram.opus.OpusLib;
import org.telegram.threading.ActorReference;
import org.telegram.threading.ActorSystem;
import org.telegram.threading.ReflectedActor;

import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class OpusPlayerActor extends ReflectedActor {

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
    private AudioPlayerActor.SubMessenger basePlayer;

    public OpusPlayerActor(ActorReference audioPlayerActor, ActorSystem system) {
        super(system, Actors.THREAD_AUDIO);
        this.basePlayer = new AudioPlayerActor.SubMessenger(audioPlayerActor, self());
        this.opusLib = new OpusLib();
    }

    protected void onPlayMessage(long id, String fileName) {
        if (state != STATE_NONE) {
            destroyPlayer();
        }
        state = STATE_NONE;
        currentFileName = fileName;
        currentId = id;

        int res = opusLib.openOpusFile(currentFileName);
        if (res == 0) {
            basePlayer.crash(currentId);
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
            basePlayer.crash(currentId);
            return;
        }

        state = STATE_STARTED;
        self().talk("iterate", self());
    }

    protected void onIterateMessage() {
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

        basePlayer.progress(currentId, scale);

        if (!isFinished) {
            self().talk("iterate", self());
        } else {
            self().talk("stop", self());
        }
    }

    protected void onPauseMessage() {
        if (state == STATE_STARTED) {
            audioTrack.pause();
            state = STATE_PAUSED;
        }
        float scale = 0;
        if (duration != 0) {
            scale = offset / (float) duration;
        }
        basePlayer.paused(currentId, scale);
    }

    protected void onResumeMessage() {
        if (state == STATE_PAUSED) {
            audioTrack.play();
            state = STATE_STARTED;
            onIterateMessage();
        }
    }

    protected void onStopMessage() {
        destroyPlayer();
        state = STATE_NONE;
        basePlayer.stoped(currentId);
    }

    protected void onToggleMessage(long id, String fileName) {
        if (state == STATE_PAUSED) {
            onResumeMessage();
        } else if (state == STATE_STARTED) {
            onPauseMessage();
        } else {
            onPlayMessage(id, fileName);
        }
    }

    private void destroyPlayer() {
        opusLib.closeOpusFile();
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }
}
