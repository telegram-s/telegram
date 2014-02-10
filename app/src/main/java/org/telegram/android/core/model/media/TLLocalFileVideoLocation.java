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
    private String uniqKey;

    public TLLocalFileVideoLocation() {

    }

    public TLLocalFileVideoLocation(int dcId, long videoId, long accessHash, int size) {
        this.dcId = dcId;
        this.videoId = videoId;
        this.accessHash = accessHash;
        this.size = size;
        this.uniqKey = dcId + "_" + videoId;
    }

    public int getDcId() {
        return dcId;
    }

    public long getVideoId() {
        return videoId;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public int getSize() {
        return size;
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
        this.uniqKey = dcId + "_" + videoId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TLLocalFileVideoLocation)) {
            return false;
        }
        return super.equals(o);
    }

    public boolean equals(TLLocalFileVideoLocation videoLocation) {
        return videoLocation.dcId == dcId &&
                videoLocation.videoId == videoId &&
                videoLocation.accessHash == accessHash &&
                videoLocation.size == size;
    }

    @Override
    public String getUniqKey() {
        return uniqKey;
    }
}
