package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalUnknown extends TLObjectCompat implements Serializable {
    private byte[] data;

    public byte[] getData() {
        return data;
    }
}
