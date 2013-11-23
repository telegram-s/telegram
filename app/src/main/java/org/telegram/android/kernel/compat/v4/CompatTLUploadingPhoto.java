package org.telegram.android.kernel.compat.v4;

import org.telegram.android.kernel.compat.v1.TLObjectCompat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 23.11.13.
 */
public class CompatTLUploadingPhoto extends TLObjectCompat implements Serializable {
    private String fileName;
    private String fileUri;
    private int width;
    private int height;

    public String getFileName() {
        return fileName;
    }

    public String getFileUri() {
        return fileUri;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
