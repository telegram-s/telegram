package org.telegram.android.auth.compat.v1;

import android.content.Context;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 2:02
 */
public class CompatCredentials extends CompatContextPersistence implements Serializable {

    private TLUserCompat user;
    private int expires;

    public TLUserCompat getUser() {
        return user;
    }

    public int getExpires() {
        return expires;
    }
}
