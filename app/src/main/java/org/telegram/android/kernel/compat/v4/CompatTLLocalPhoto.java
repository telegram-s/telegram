package org.telegram.android.kernel.compat.v4;

import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalPhoto extends TLObjectCompat implements Serializable {

    private int fastPreviewW;
    private int fastPreviewH;
    private byte[] fastPreview;
    private String fastPreviewKey;

    private int fullW;
    private int fullH;
    private CompatTLAbsLocalFileLocation fullLocation;

    public int getFastPreviewW() {
        return fastPreviewW;
    }

    public int getFastPreviewH() {
        return fastPreviewH;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }

    public String getFastPreviewKey() {
        return fastPreviewKey;
    }

    public int getFullW() {
        return fullW;
    }

    public int getFullH() {
        return fullH;
    }

    public CompatTLAbsLocalFileLocation getFullLocation() {
        return fullLocation;
    }
}
