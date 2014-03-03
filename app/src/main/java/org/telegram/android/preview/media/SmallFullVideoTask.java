package org.telegram.android.preview.media;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class SmallFullVideoTask extends BaseTask {
    private String fileName;

    public SmallFullVideoTask(String fileName) {
        super(true);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getStorageKey() {
        return "s:" + fileName;
    }
}
