package org.telegram.android.preview.media;

import org.telegram.android.preview.queue.QueueProcessor;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class FullFileTask extends QueueProcessor.BaseTask {

    private String fileName;

    public FullFileTask(String fileName) {
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
