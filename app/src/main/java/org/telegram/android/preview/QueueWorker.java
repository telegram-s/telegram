package org.telegram.android.preview;

import android.os.*;
import android.os.Process;

/**
 * Created by ex3ndr on 05.02.14.
 */
public abstract class QueueWorker<T extends QueueProcessor.BaseTask> extends Thread implements QueueProcessor.TaskFilter<T> {

    private QueueProcessor<T> processor;

    public QueueWorker(QueueProcessor<T> processor) {
        this.processor = processor;
    }

    protected abstract boolean processTask(T task);

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (processor.isRunning()) {
            T task = processor.waitForTask(this);
            if (task == null) {
                continue;
            }

            if (processTask(task)) {
                processor.taskCompleted(task);
            } else {
                processor.returnTaskToQueue(task);
            }
        }
    }
}
