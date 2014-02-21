package org.telegram.android.preview.queue;

/**
 * Created by ex3ndr on 05.02.14.
 */
public abstract class QueueWorker<T extends QueueProcessor.BaseTask> extends Thread implements QueueProcessor.TaskFilter<T> {

    private QueueProcessor<T> processor;

    public QueueWorker(QueueProcessor<T> processor) {
        this.processor = processor;
    }

    protected abstract boolean processTask(T task) throws Exception;

    protected boolean needRepeatOnError() {
        return false;
    }

    @Override
    public void run() {
        setPriority(MIN_PRIORITY);
        //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_m;
        while (processor.isRunning()) {
            T task = processor.waitForTask(this);
            if (task == null) {
                continue;
            }

            try {
                if (processTask(task)) {
                    processor.taskCompleted(task);
                } else {
                    processor.returnTaskToQueue(task);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (needRepeatOnError()) {
                    processor.returnTaskToQueue(task);
                }
            }
        }
    }
}
