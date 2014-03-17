package org.telegram.android.core.audio;

import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class FileWriterActor extends Actor<FileWriterActor.Message> {

    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_COMPLETED = 2;

    private int state = STATE_NONE;

    private FileOutputStream outputStream;

    public FileWriterActor(ActorSystem system) {
        super(system, "fs");
    }

    @Override
    public void receive(Message message, Actor sender) {
        if (message instanceof StartMessage) {
            if (state != STATE_NONE) {
                return;
            }

            try {
                outputStream = new FileOutputStream(((StartMessage) message).fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // TODO??
            }
            state = STATE_STARTED;
        } else if (message instanceof WriteMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            WriteMessage writeMessage = (WriteMessage) message;
            try {
                outputStream.write(writeMessage.buffer, 0, writeMessage.size);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (message instanceof StopMessage) {
            if (state != STATE_STARTED) {
                return;
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
