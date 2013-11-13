package org.telegram.android.auth.compat.v1;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 2:41
 */
public class TLFileLocationCompat extends TLFileLocationAbsCompat {
    protected int dcId;
    protected long volumeId;
    protected int localId;
    protected long secret;

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
}
