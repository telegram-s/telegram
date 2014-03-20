package org.telegram.android.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.os.Vibrator;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class VoiceCaptureActor extends Actor<VoiceCaptureActor.Message> {

    private static final int BUFFER_SIZE = 16 * 1024;

    private static final int STATE_STOPPED = 0;
    private static final int STATE_STARTED = 1;

    private int state = STATE_STOPPED;

    private AudioRecord audioRecord;
    private String fileName;
    // private FileWriterActor writerActor;
    private OpusEncoder opusActor;
    private int bufferSize;
    private TelegramApplication application;
    private long actionId;
    private long playStartTime;
    private long lastNotificationTime;

    public VoiceCaptureActor(TelegramApplication application, ActorSystem system) {
        super(system, "audio");
        this.application = application;
    }

    public boolean isStarted() {
        return state == STATE_STARTED;
    }

    @Override
    public void receive(VoiceCaptureActor.Message message, Actor sender) {
        if (message instanceof StartMessage) {
            if (state == STATE_STARTED) {
                return;
            }
            fileName = ((StartMessage) message).fileName;
            actionId = ((StartMessage) message).id;

            int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            bufferSize = 16 * minBufferSize;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();
            opusActor = new OpusEncoder(actorSystem);
            opusActor.sendMessage(new OpusEncoder.StartMessage(fileName));
            // writerActor = new FileWriterActor(actorSystem);
            // writerActor.sendMessage(new FileWriterActor.StartMessage(fileName));
            state = STATE_STARTED;
            playStartTime = SystemClock.uptimeMillis();
            vibrate();
            sendMessage(new ReadMessage());
        } else if (message instanceof ReadMessage) {
            if (state != STATE_STARTED) {
                return;
            }

            byte[] buffer = VoiceBuffers.getInstance().obtainBuffer(BUFFER_SIZE);
            int len = audioRecord.read(buffer, 0, buffer.length);
            if (len > 0) {
                // writerActor.sendMessage(new FileWriterActor.WriteMessage(buffer, len));
                opusActor.sendMessage(new OpusEncoder.WriteMessage(buffer, len));
            } else {
                VoiceBuffers.getInstance().releaseBuffer(buffer);
            }

            application.getKernel().getUiKernel().getUiNotifications().sendState(
                    Events.KIND_AUDIO_RECORD,
                    actionId,
                    Events.STATE_IN_PROGRESS,
                    (Long) (SystemClock.uptimeMillis() - playStartTime));
            sendMessage(new ReadMessage());
        } else if (message instanceof StopMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            opusActor.sendMessage(new OpusEncoder.StopMessage());

            application.getKernel().getUiKernel().getUiNotifications().sendState(
                    Events.KIND_AUDIO_RECORD,
                    actionId,
                    Events.STATE_STOP,
                    (Long) (SystemClock.uptimeMillis() - playStartTime));
            state = STATE_STOPPED;
        } else if (message instanceof CrashMessage) {
//            if (state != STATE_STARTED) {
//                return;
//            }
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            if (opusActor != null) {
                opusActor.sendMessage(new OpusEncoder.StopMessage());
            }

            application.getKernel().getUiKernel().getUiNotifications().sendState(
                    Events.KIND_AUDIO_RECORD,
                    actionId,
                    Events.STATE_ERROR);

            state = STATE_STOPPED;
        }
    }

    public static abstract class Message {

    }

    public static class StartMessage extends Message {
        private static final AtomicInteger lastStateId = new AtomicInteger();
        public String fileName;
        public long id;

        public StartMessage(String fileName) {
            this.fileName = fileName;
            this.id = lastStateId.incrementAndGet();
        }
    }

    public static class StopMessage extends Message {

    }

    public static class ReadMessage extends Message {

    }

    public static class CrashMessage extends Message {

    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(20);
        } catch (Exception e) {
        }
    }
}
