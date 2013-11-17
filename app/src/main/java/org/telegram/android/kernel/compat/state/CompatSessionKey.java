package org.telegram.android.kernel.compat.state;

import java.io.Serializable;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatSessionKey implements Serializable {
    private int dcId;
    private byte[] session;
    private int seqNo;
    private long lastMessageId;

    public int getDcId() {
        return dcId;
    }

    public byte[] getSession() {
        return session;
    }

    public int getSeqNo() {
        return seqNo;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }
}
