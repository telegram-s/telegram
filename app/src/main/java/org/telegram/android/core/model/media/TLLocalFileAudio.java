package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 12.01.14.
 */
public class TLLocalFileAudio extends TLAbsLocalFileLocation {

    public static final int CLASS_ID = 0x0325d07e;

    private long id;
    private long accessHash;
    private int size;
    private int dcId;

    public TLLocalFileAudio(long id, long accessHash, int size, int dcId) {
        this.id = id;
        this.accessHash = accessHash;
        this.size = size;
        this.dcId = dcId;
    }

    public TLLocalFileAudio() {

    }

    public long getId() {
        return id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public int getSize() {
        return size;
    }

    public int getDcId() {
        return dcId;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(id, stream);
        writeLong(accessHash, stream);
        writeInt(size, stream);
        writeInt(dcId, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        id = readLong(stream);
        accessHash = readLong(stream);
        size = readInt(stream);
        dcId = readInt(stream);
    }
}
