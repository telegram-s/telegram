package org.telegram.android.kernel.compat.v4;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalFileVideoLocation extends CompatTLAbsLocalFileLocation implements Serializable {
    protected int dcId;
    protected long videoId;
    protected long accessHash;
    protected int size;

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
}
