package org.telegram.android.util;

import org.telegram.mtproto.secure.pq.PQImplementation;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class NativePQ implements PQImplementation {

    static {
        System.loadLibrary("timg");
    }

    @Override
    public long findDivider(long src) {
        return 0;
    }

    private native long solvePq(long src);
}
