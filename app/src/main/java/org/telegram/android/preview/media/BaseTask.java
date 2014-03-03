package org.telegram.android.preview.media;

import org.telegram.android.preview.queue.QueueProcessor;

/**
 * Created by ex3ndr on 22.02.14.
 */
public abstract class BaseTask extends QueueProcessor.BaseTask {
    private boolean isOut;

    protected BaseTask(boolean isOut) {
        this.isOut = isOut;
    }

    public boolean isOut() {
        return isOut;
    }

    protected abstract String getStorageKey();

    @Override
    public final String getKey() {
        return isOut() + ":" + getStorageKey();
    }
}
