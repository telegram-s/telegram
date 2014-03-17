package org.telegram.android.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Vibrator;
import org.telegram.android.TelegramApplication;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

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
    private FileWriterActor writerActor;
    private int bufferSize;
    private TelegramApplication application;

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
            int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            bufferSize = 16 * minBufferSize;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();
            writerActor = new FileWriterActor(actorSystem);
            writerActor.sendMessage(new FileWriterActor.StartMessage(((StartMessage) message).fileName));
            state = STATE_STARTED;
            vibrate();
            sendMessage(new ReadMessage());
        } else if (message instanceof ReadMessage) {
            if (state != STATE_STARTED) {
                return;
            }

            byte[] buffer = VoiceBuffers.getInstance().obtainBuffer(BUFFER_SIZE);
            int len = audioRecord.read(buffer, 0, buffer.length);
            if (len > 0) {
                // Forward data to child actor
                writerActor.sendMessage(new FileWriterActor.WriteMessage(buffer, len));
            } else {
                VoiceBuffers.getInstance().releaseBuffer(buffer);
            }
            sendMessage(new ReadMessage());
        } else if (message instanceof StopMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            audioRecord.stop();
            audioRecord = null;
            writerActor.sendMessage(new FileWriterActor.StopMessage());
            state = STATE_STOPPED;
        }
    }

    public static abstract class Message {

    }

    public static class StartMessage extends Message {
        public String fileName;

        public StartMessage(String fileName) {
            this.fileName = fileName;
        }
    }

    public static class StopMessage extends Message {

    }

    public static class ReadMessage extends Message {

    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(20);
        } catch (Exception e) {
        }
    }
}
