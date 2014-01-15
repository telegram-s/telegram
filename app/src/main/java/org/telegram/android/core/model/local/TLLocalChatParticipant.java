package org.telegram.android.core.model.local;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 15.01.14.
 */
public class TLLocalChatParticipant extends TLObject {

    public static final int CLASS_ID = 0x3f9c109f;

    private int uid;
    private int inviter;
    private int date;

    public TLLocalChatParticipant(int uid, int inviter, int date) {
        this.uid = uid;
        this.inviter = inviter;
        this.date = date;
    }

    public TLLocalChatParticipant() {

    }

    public int getUid() {
        return uid;
    }

    public int getInviter() {
        return inviter;
    }

    public int getDate() {
        return date;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(uid, stream);
        writeInt(inviter, stream);
        writeInt(date, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        uid = readInt(stream);
        inviter = readInt(stream);
        date = readInt(stream);
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
