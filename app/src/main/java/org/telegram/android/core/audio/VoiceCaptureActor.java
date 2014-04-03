package org.telegram.android.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.os.Vibrator;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
import org.telegram.actors.ActorMessenger;
import org.telegram.actors.ActorReference;
import org.telegram.actors.ActorSystem;
import org.telegram.actors.ReflectedActor;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class VoiceCaptureActor extends ReflectedActor {

    public static final AtomicInteger LAST_ID = new AtomicInteger(0);

    private static final int BUFFER_SIZE = 16 * 1024;

    private static final int STATE_STOPPED = 0;
    private static final int STATE_STARTED = 1;

    private int state = STATE_STOPPED;

    private AudioRecord audioRecord;
    private OpusEncoderActor.Messenger opusActor;
    private int bufferSize;
    private TelegramApplication application;
    private long actionId;
    private long playStartTime;

    public VoiceCaptureActor(TelegramApplication application, ActorSystem system) {
        super(system, "voice_capture", "audio");
        this.application = application;
    }

    protected void onStartMessage(long id, String fileName) {
        if (state == STATE_STARTED) {
            return;
        }
        actionId = id;

        int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufferSize = 16 * minBufferSize;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        audioRecord.startRecording();
        opusActor = new OpusEncoderActor.Messenger(new OpusEncoderActor(actorSystem).self(), self());
        opusActor.start(fileName);
        state = STATE_STARTED;
        playStartTime = SystemClock.uptimeMillis();
        vibrate();
        self().talk("iterate", self());
    }

    protected void onIterateMessage() {
        if (state != STATE_STARTED) {
            return;
        }

        byte[] buffer = VoiceBuffers.getInstance().obtainBuffer(BUFFER_SIZE);
        int len = audioRecord.read(buffer, 0, buffer.length);
        if (len > 0) {
            opusActor.write(buffer, len);
        } else {
            VoiceBuffers.getInstance().releaseBuffer(buffer);
        }

        application.getKernel().getUiKernel().getUiNotifications().sendState(
                Events.KIND_AUDIO_RECORD,
                actionId,
                Events.STATE_IN_PROGRESS,
                (Long) (SystemClock.uptimeMillis() - playStartTime));
        self().talk("iterate", self());
    }

    protected void onStopMessage() {
        if (state != STATE_STARTED) {
            return;
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        opusActor.stop();

        application.getKernel().getUiKernel().getUiNotifications().sendState(
                Events.KIND_AUDIO_RECORD,
                actionId,
                Events.STATE_STOP,
                (Long) (SystemClock.uptimeMillis() - playStartTime));
        state = STATE_STOPPED;
    }

    protected void onCrashMessage() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (opusActor != null) {
            opusActor.stop();
        }

        application.getKernel().getUiKernel().getUiNotifications().sendState(
                Events.KIND_AUDIO_RECORD,
                actionId,
                Events.STATE_ERROR);

        state = STATE_STOPPED;
    }

    @Override
    public void onException(Exception e) {
        e.printStackTrace();
        onCrashMessage();
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(20);
        } catch (Exception e) {
        }
    }

    public static class Messenger extends ActorMessenger {

        public Messenger(ActorReference reference, ActorReference sender) {
            super(reference, sender);
        }

        public void start(long id, String fileName) {
            talkRaw("start", id, fileName);
        }

        public void stop() {
            talkRaw("stop");
        }

        @Override
        public ActorMessenger cloneForSender(ActorReference sender) {
            return new Messenger(reference, sender);
        }
    }
}
