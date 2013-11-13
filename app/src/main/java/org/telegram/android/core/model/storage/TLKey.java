package org.telegram.android.core.model.storage;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 08.11.13
 * Time: 23:33
 */
public class TLKey extends TLObject {

    public static final int CLASS_ID = 0x4a861b1a;

    private int dcId;

    private byte[] authKey;

    private boolean isAuthorised;

    private TLVector<TLLastKnownSalt> salts;

    private TLVector<TLOldSession> oldSessions;

    public TLKey(int dcId, byte[] authKey) {
        this.dcId = dcId;
        this.authKey = authKey;
        this.isAuthorised = false;
        this.salts = new TLVector<TLLastKnownSalt>();
        this.oldSessions = new TLVector<TLOldSession>();
    }

    public TLKey() {

    }

    public int getDcId() {
        return dcId;
    }

    public byte[] getAuthKey() {
        return authKey;
    }

    public boolean isAuthorised() {
        return isAuthorised;
    }

    public void setAuthorised(boolean authorised) {
        isAuthorised = authorised;
    }

    public TLVector<TLLastKnownSalt> getSalts() {
        return salts;
    }

    public TLVector<TLOldSession> getOldSessions() {
        return oldSessions;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(dcId, stream);
        writeByteArray(authKey, stream);
        writeTLBool(isAuthorised, stream);
        writeTLVector(salts, stream);
        writeTLVector(oldSessions, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        dcId = readInt(stream);
        authKey = readBytes(256, stream);
        isAuthorised = readTLBool(stream);
        salts = readTLVector(stream, context);
        oldSessions = readTLVector(stream, context);
    }
}
