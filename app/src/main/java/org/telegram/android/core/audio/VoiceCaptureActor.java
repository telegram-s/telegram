package org.telegram.android.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Vibrator;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;
import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.nio.ByteBuffer;

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
            int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            bufferSize = 16 * minBufferSize;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();
            opusActor = new OpusEncoder(actorSystem);
            opusActor.sendMessage(new OpusEncoder.StartMessage(fileName + ".opus"));
            // writerActor = new FileWriterActor(actorSystem);
            // writerActor.sendMessage(new FileWriterActor.StartMessage(fileName));
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
                // writerActor.sendMessage(new FileWriterActor.WriteMessage(buffer, len));
                opusActor.sendMessage(new OpusEncoder.WriteMessage(buffer, len));
            } else {
                VoiceBuffers.getInstance().releaseBuffer(buffer);
            }
            sendMessage(new ReadMessage());
        } else if (message instanceof StopMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            opusActor.sendMessage(new OpusEncoder.StopMessage());
            // writerActor.sendMessage(new FileWriterActor.StopMessage());
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
