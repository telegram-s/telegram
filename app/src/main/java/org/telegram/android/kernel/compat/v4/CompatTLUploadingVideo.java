package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLUploadingVideo extends TLObjectCompat implements Serializable {
    private String fileName;
    private int previewWidth;
    private int previewHeight;

    public String getFileName() {
        return fileName;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }
}
