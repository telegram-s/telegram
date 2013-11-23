package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalGeo extends TLObjectCompat implements Serializable {
    private double latitude;
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
