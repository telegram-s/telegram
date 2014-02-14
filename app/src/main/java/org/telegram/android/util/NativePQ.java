package org.telegram.android.util;

import org.telegram.android.log.Logger;
import org.telegram.mtproto.secure.pq.PQImplementation;
import org.telegram.mtproto.secure.pq.PQLopatin;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class NativePQ implements PQImplementation {

    private static final String TAG = "NativePQ";

    static {
        System.loadLibrary("timg");
    }

    @Override
    public long findDivider(long src) {
        long res = solvePq(src);
        res = Math.min(res, src / res);
        // long javaRes = new PQLopatin().findDivider(src);
        // Logger.d(TAG, "pq:" + res + ", " + javaRes);
        return res;
    }

    private native long solvePq(long src);
}
