package org.telegram.android.core.files;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 16:54
 */
public class UploadResult {
    private int partsCount;
    private String hash;

    public UploadResult(String hash, int partsCount) {
        this.hash = hash;
        this.partsCount = partsCount;
    }

    public int getPartsCount() {
        return partsCount;
    }

    public String getHash() {
        return hash;
    }
}
