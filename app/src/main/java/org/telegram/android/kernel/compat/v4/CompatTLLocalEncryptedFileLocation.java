package org.telegram.android.kernel.compat.v4;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalEncryptedFileLocation extends CompatTLAbsLocalFileLocation implements Serializable {
    private long id;
    private long accessHash;
    private int size;
    private int dcId;
    private byte[] key;
    private byte[] iv;

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

    public byte[] getKey() {
        return key;
    }

    public byte[] getIv() {
        return iv;
    }
}
