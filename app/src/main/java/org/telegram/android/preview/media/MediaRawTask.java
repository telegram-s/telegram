package org.telegram.android.preview.media;

/**
* Created by ex3ndr on 22.02.14.
*/
public class MediaRawTask extends BaseTask {
    private String fileName;

    public MediaRawTask(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getKey() {
        return fileName;
    }
}
