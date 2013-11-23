package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLLocalVideo extends TLObjectCompat implements Serializable {
    private int duration;
    private CompatTLAbsLocalFileLocation videoLocation;

    private int previewW;
    private int previewH;
    private String previewKey;
    private CompatTLAbsLocalFileLocation previewLocation;
    private byte[] fastPreview;

    public int getDuration() {
        return duration;
    }

    public CompatTLAbsLocalFileLocation getVideoLocation() {
        return videoLocation;
    }

    public int getPreviewW() {
        return previewW;
    }

    public int getPreviewH() {
        return previewH;
    }

    public String getPreviewKey() {
        return previewKey;
    }

    public CompatTLAbsLocalFileLocation getPreviewLocation() {
        return previewLocation;
    }

    public byte[] getFastPreview() {
        return fastPreview;
    }
}
