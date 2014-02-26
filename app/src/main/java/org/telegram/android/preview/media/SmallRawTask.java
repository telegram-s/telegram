package org.telegram.android.preview.media;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallRawTask extends BaseTask {
    private String fileName;

    public SmallRawTask(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getKey() {
        return "s:" + fileName;
    }
}
