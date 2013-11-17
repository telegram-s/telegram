package org.telegram.android.kernel.compat.state;

import java.io.Serializable;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatDcKey implements Serializable {
    private boolean isAuthorized;
    private byte[] authKey;
    private byte[] serverSalt;

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public byte[] getAuthKey() {
        return authKey;
    }

    public byte[] getServerSalt() {
        return serverSalt;
    }
}
