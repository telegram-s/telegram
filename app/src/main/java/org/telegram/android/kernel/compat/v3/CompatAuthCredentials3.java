package org.telegram.android.kernel.compat.v3;

import org.telegram.android.kernel.compat.CompatContextPersistence;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatAuthCredentials3 extends CompatContextPersistence {
    private int uid;
    private int expires;
    private boolean isLoginned;

    public int getUid() {
        return uid;
    }

    public int getExpires() {
        return expires;
    }

    public boolean isLoginned() {
        return isLoginned;
    }
}
