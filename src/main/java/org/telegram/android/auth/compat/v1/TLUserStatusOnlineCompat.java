package org.telegram.android.auth.compat.v1;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 2:16
 */
public class TLUserStatusOnlineCompat extends TLUserStatusCompat {
    private int expires;

    public int getExpires() {
        return expires;
    }
}
