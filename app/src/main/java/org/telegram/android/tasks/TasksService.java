package org.telegram.android.tasks;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 1:25
 */
public class TasksService {
    private Handler checkHandler;
    private Thread checkingThread;
    private ConcurrentHashMap<Integer, TaskHandler> callbacks;
    private static AtomicInteger nextTaskId = new AtomicInteger(1);

    public TasksService() {
        callbacks = new ConcurrentHashMap<Integer, TaskHandler>();
        checkingThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                checkHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (callbacks.containsKey(msg.what)) {
                            final TaskHandler handler = callbacks.get(msg.what);
                            synchronized (handler) {
                                if (handler.getTaskState() == TaskState.IN_PROGRESS) {
                                    if (SystemClock.uptimeMillis() - handler.getStartTime() > handler.getTimeOut()) {
                                        handler.setTaskState(TaskState.TIMED_OUT);
                                        checkHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                handler.getCallback().onTimedOut();
                                            }
                                        });
                                    } else {
                                        checkHandler.sendEmptyMessageDelayed(msg.what, SystemClock.uptimeMillis() - handler.getStartTime());
                                    }
                                }
                            }
                        }
                    }
                };

                Looper.loop();
            }
        };
        checkingThread.setName("TasksCheckingThread#" + checkingThread.hashCode());
        checkingThread.start();
        while (checkHandler == null) {
            Thread.yield();
        }
    }

    public int pushTask(TaskCallback callback, long timeout) {
        int taskId = nextTaskId.getAndIncrement();
        callbacks.put(taskId, new TaskHandler(callback, SystemClock.uptimeMillis(), timeout));
        checkHandler.sendEmptyMessageDelayed(taskId, timeout);
        return taskId;
    }

    public void onTaskCancelled(int taskId) {
        if (callbacks.containsKey(taskId)) {
            final TaskHandler handler = callbacks.get(taskId);
            synchronized (handler) {
                if (handler.getTaskState() == TaskState.IN_PROGRESS) {
                    handler.setTaskState(TaskState.CANCELLED);
                    checkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.getCallback().onCanceled();
                        }
                    });
                }
            }
        }
    }

    public void onTaskFailure(int taskId, final Exception e) {
        if (callbacks.containsKey(taskId)) {
            final TaskHandler handler = callbacks.get(taskId);
            synchronized (handler) {
                if (handler.getTaskState() == TaskState.IN_PROGRESS) {
                    handler.setTaskState(TaskState.FAILURE);
                    checkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.getCallback().onException(e);
                        }
                    });
                }
            }
        }
    }

    public void onTaskCompleted(int taskId, final Object value) {
        if (callbacks.containsKey(taskId)) {
            final TaskHandler handler = callbacks.get(taskId);
            synchronized (handler) {
                if (handler.getTaskState() == TaskState.IN_PROGRESS) {
                    handler.setTaskState(TaskState.SUCCESS);
                    handler.setTaskResult(value);
                    checkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.getCallback().onSuccess(value);
                        }
                    });
                }
            }
        }
    }

    public void onTaskConfirmed(int taskId) {
        if (callbacks.containsKey(taskId)) {
            final TaskHandler handler = callbacks.get(taskId);
            synchronized (handler) {
                if (handler.getTaskState() == TaskState.IN_PROGRESS) {
                    checkHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handler.getCallback().onConfirmed();
                        }
                    });
                }
            }
        }
    }

    public void forgetTask(int taskId) {
        callbacks.remove(taskId);
    }
}