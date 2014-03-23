package org.telegram.android.core.audio;

import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;
import org.telegram.threading.ReflectedActor;

import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class OpusEncoder extends ReflectedActor {
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_COMPLETED = 2;

    private int state = STATE_NONE;

    private OpusLib opusLib = new OpusLib();

    private ByteBuffer fileBuffer = ByteBuffer.allocateDirect(1920);

    public OpusEncoder(ActorSystem system) {
        super(system, "encoding");
    }

    protected void onStartMessage(String fileName) {
        if (state != STATE_NONE) {
            return;
        }
        int result = opusLib.startRecord(fileName);
        state = STATE_STARTED;
    }

    protected void onWriteMessage(byte[] buffer, int size) {
        if (state != STATE_STARTED) {
            return;
        }
        ByteBuffer finalBuffer = ByteBuffer.allocateDirect(size);
        finalBuffer.put(buffer, 0, size);
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
                int length = !flush ? fileBuffer.limit() : finalBuffer.position();
                if (opusLib.writeFrame(fileBuffer, length) != 0) {
                    fileBuffer.rewind();
                }
            }
            if (oldLimit != -1) {
                finalBuffer.limit(oldLimit);
            }
        }
    }

    protected void onStopMessage() {
        if (state != STATE_STARTED) {
            return;
        }

        opusLib.stopRecord();

        state = STATE_COMPLETED;
    }
}
