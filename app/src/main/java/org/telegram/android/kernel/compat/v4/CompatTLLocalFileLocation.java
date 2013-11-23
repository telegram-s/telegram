package org.telegram.android.kernel.compat.v4;

import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalFileLocation extends CompatTLAbsLocalFileLocation implements Serializable {
    protected int dcId;
    protected long volumeId;
    protected int localId;
    protected long secret;
    protected int size;

    public int getDcId() {
        return dcId;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public int getLocalId() {
        return localId;
    }

    public long getSecret() {
        return secret;
    }

    public int getSize() {
        return size;
    }
}
