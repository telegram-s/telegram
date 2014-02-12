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
        long res = solvePq(src);
        res = Math.min(res, src / res);
        return res;
    }

    private native long solvePq(long src);
}
