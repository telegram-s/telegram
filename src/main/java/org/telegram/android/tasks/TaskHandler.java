package org.telegram.android.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 1:32
 */
public class TaskHandler {
    private TaskCallback callback;
    private long startTime;
    private long timeOut;
    private TaskState taskState;
    private Object taskResult;

    public TaskHandler(TaskCallback callback, long startTime, long timeOut) {
        this.callback = callback;
        this.startTime = startTime;
        this.timeOut = timeOut;
        this.taskState = TaskState.IN_PROGRESS;
    }

    public TaskState getTaskState() {
        return taskState;
    }

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    public Object getTaskResult() {
        return taskResult;
    }

    public void setTaskResult(Object taskResult) {
        this.taskResult = taskResult;
    }

    public TaskCallback getCallback() {
        return callback;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getTimeOut() {
        return timeOut;
    }
}
