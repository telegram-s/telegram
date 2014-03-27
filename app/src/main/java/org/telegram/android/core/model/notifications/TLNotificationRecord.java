package org.telegram.android.core.model.notifications;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 27.03.14.
 */
public class TLNotificationRecord extends TLObject {
    public static final int CLASS_ID = 0xe755a921;

    private int peerType;
    private int peerId;
    private int senderId;
    private String contentShortMessage;
    private String contentMessage;

    public TLNotificationRecord(int peerType, int peerId, int senderId, String contentShortMessage, String contentMessage) {
        this.peerType = peerType;
        this.peerId = peerId;
        this.contentShortMessage = contentShortMessage;
        this.contentMessage = contentMessage;
        this.senderId = senderId;
    }

    public TLNotificationRecord() {

    }

    public int getPeerType() {
        return peerType;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getContentShortMessage() {
        return contentShortMessage;
    }

    public String getContentMessage() {
        return contentMessage;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(peerType, stream);
        writeInt(peerId, stream);
        writeTLString(contentShortMessage, stream);
        writeTLString(contentMessage, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        peerType = readInt(stream);
        peerId = readInt(stream);
        contentShortMessage = readTLString(stream);
        contentMessage = readTLString(stream);
    }
}
