package com.extradea.framework.images;

/**
 * Author: Korshakov Stepan
 * Created: 01.07.13 9:23
 */
public class TaskEvent {
    public static final int EVENT_CREATED = 0;
    public static final int EVENT_STARTED = 1;
    public static final int EVENT_FINISHED = 2;
    public static final int EVENT_FINISHED_WITH_ERROR = 3;
    public static final int EVENT_REMOVED = 4;
    public static final int EVENT_DOWNLOADING = 5;

    public static final int EVENT_CUSTOM_START = 100;

    private String taskKey;
    private int eventId;
    private Object[] args;

    public TaskEvent(String taskKey, int eventId, Object[] args) {
        this.taskKey = taskKey;
        this.eventId = eventId;
        this.args = args;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public int getEventId() {
        return eventId;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
