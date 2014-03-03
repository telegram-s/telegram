package org.telegram.android.preview.media;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class MediaVideoTask extends BaseTask {
    private String fileName;

    public MediaVideoTask(String fileName, boolean isOut) {
        super(isOut);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getStorageKey() {
        return fileName;
    }
}
