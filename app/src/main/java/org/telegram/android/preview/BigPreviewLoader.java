package org.telegram.android.preview;

import org.telegram.android.TelegramApplication;
import org.telegram.android.preview.media.FullFileTask;
import org.telegram.android.preview.queue.QueueWorker;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class BigPreviewLoader extends BaseLoader<FullFileTask> {
    public BigPreviewLoader(TelegramApplication application) {
        super("full_preview", 1, application);
    }

    @Override
    protected QueueWorker<FullFileTask>[] createWorkers() {
        return new QueueWorker[]{

        };
    }
}
