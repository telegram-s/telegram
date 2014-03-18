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
    private FileWriterActor writerActor;
    private int bufferSize;
    private TelegramApplication application;
    private OpusLib opusLib = new OpusLib();
    private ByteBuffer fileBuffer = ByteBuffer.allocateDirect(1920);

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
            writerActor = new FileWriterActor(actorSystem);
            writerActor.sendMessage(new FileWriterActor.StartMessage(fileName));
            int res = opusLib.startRecord(fileName + ".opus");
            Logger.d("VoiceCapture", "Start record: " + res);
            fileBuffer.rewind();
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
                ByteBuffer finalBuffer = ByteBuffer.allocateDirect(len);
                finalBuffer.put(buffer, 0, len);
                finalBuffer.rewind();
                boolean flush = false;

                while (finalBuffer.hasRemaining()) {
                    int oldLimit = -1;
                    if (finalBuffer.remaining() > fileBuffer.remaining()) {
                        oldLimit = finalBuffer.limit();
                        finalBuffer.limit(fileBuffer.remaining() + finalBuffer.position());
                    }
                    fileBuffer.put(finalBuffer);
                    if (fileBuffer.position() == fileBuffer.limit() || flush) {
                        if (opusLib.writeFrame(fileBuffer, !flush ? fileBuffer.limit() : finalBuffer.position()) != 0) {
                            fileBuffer.rewind();
                            // recordTimeCount += fileBuffer.limit() / 2 / 16;
                        }
                    }
                    if (oldLimit != -1) {
                        finalBuffer.limit(oldLimit);
                    }
                }

                // int res = opusLib.writeFrame(byteBuffer, len);
                // Logger.d("VoiceCapture", "Write frame: " + res);
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
            opusLib.stopRecord();
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
