package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 2:40
 */
public class TLLocalFileVideoLocation extends TLAbsLocalFileLocation {

    public static final int CLASS_ID = 0x2cdc079e;

    protected int dcId;
    protected long videoId;
    protected long accessHash;
    protected int size;

    public TLLocalFileVideoLocation() {

    }

    public TLLocalFileVideoLocation(int dcId, long videoId, long accessHash, int size) {
        this.dcId = dcId;
        this.videoId = videoId;
        this.accessHash = accessHash;
        this.size = size;
    }

    public int getDcId() {
        return dcId;
    }

    public void setDcId(int dcId) {
        this.dcId = dcId;
    }

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
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
        writeLong(videoId, stream);
        writeLong(accessHash, stream);
        writeInt(size, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        dcId = readInt(stream);
        videoId = readLong(stream);
        accessHash = readLong(stream);
        size = readInt(stream);
    }
}
