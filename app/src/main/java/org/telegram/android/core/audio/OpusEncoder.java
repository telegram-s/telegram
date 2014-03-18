package org.telegram.android.core.audio;

import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.nio.ByteBuffer;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class OpusEncoder extends Actor<OpusEncoder.Message> {
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_COMPLETED = 2;

    private int state = STATE_NONE;

    private OpusLib opusLib = new OpusLib();

    private ByteBuffer fileBuffer = ByteBuffer.allocateDirect(1920);

    public OpusEncoder(ActorSystem system) {
        super(system, "encoding");
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof StartMessage) {
            if (state != STATE_NONE) {
                return;
            }
            int result = opusLib.startRecord(((StartMessage) message).fileName);
            state = STATE_STARTED;
        } else if (message instanceof WriteMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            WriteMessage writeMessage = (WriteMessage) message;
            ByteBuffer finalBuffer = ByteBuffer.allocateDirect(writeMessage.size);
            finalBuffer.put(writeMessage.buffer, 0, writeMessage.size);
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
        } else if (message instanceof StopMessage) {
            if (state != STATE_STARTED) {
                return;
            }

            opusLib.stopRecord();

            state = STATE_COMPLETED;
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

    public static class WriteMessage extends Message {
        public byte[] buffer;
        public int size;

        public WriteMessage(byte[] buffer, int size) {
            this.buffer = buffer;
            this.size = size;
        }

        public WriteMessage(byte[] buffer) {
            this.buffer = buffer;
            this.size = buffer.length;
        }
    }
}
