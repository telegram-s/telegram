package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 2:40
 */
public class TLLocalFileLocation extends TLAbsLocalFileLocation implements Serializable {

    public static final int CLASS_ID = 0x7e0b4f19;

    protected int dcId;
    protected long volumeId;
    protected int localId;
    protected long secret;
    protected int size;

    public TLLocalFileLocation() {

    }

    public TLLocalFileLocation(int dcId, long volumeId, int localId, long secret, int size) {
        this.dcId = dcId;
        this.volumeId = volumeId;
        this.localId = localId;
        this.secret = secret;
        this.size = size;
    }

    public int getDcId() {
        return dcId;
    }

    public void setDcId(int dcId) {
        this.dcId = dcId;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public long getSecret() {
        return secret;
    }

    public void setSecret(long secret) {
        this.secret = secret;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(dcId, stream);
        writeLong(volumeId, stream);
        writeInt(localId, stream);
        writeLong(secret, stream);
        writeInt(size, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        dcId = readInt(stream);
        volumeId = readLong(stream);
        localId = readInt(stream);
        secret = readLong(stream);
        size = readInt(stream);
    }
}
