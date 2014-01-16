package org.telegram.android.core.background.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ex3ndr on 16.01.14.
 */
public abstract class TaskExecutor<T> {

    private final HashSet<Long> executingTasks = new HashSet<Long>();
    private final ArrayList<TaskHolder> taskQueue = new ArrayList<TaskHolder>();
    private boolean isDestroyed;
    private List<TaskThread> taskThreads;

    public TaskExecutor(int threadCount) {
        this.isDestroyed = false;
        this.taskThreads = new ArrayList<TaskThread>();
        for (int i = 0; i < threadCount; i++) {
            TaskThread thread = new TaskThread();
            thread.start();
            this.taskThreads.add(thread);
        }
    }

    public void requestTask(long id, T task) {
        boolean hasTask = false;
        synchronized (taskQueue) {

            for (TaskHolder holder : taskQueue) {
                if (holder.taskKey == id) {
                    taskQueue.remove(holder);
                    hasTask = true;
                    break;
                }
            }
            taskQueue.add(0, new TaskHolder(id, task));
            executingTasks.add(id);
            taskQueue.notifyAll();
        }
        if (!hasTask) {
            onTaskStart(id, task);
        }
    }

    public void removeTask(long id) {
        boolean hasTask = false;
        TaskHolder foundedHolder = null;
        synchronized (taskQueue) {
            for (TaskHolder holder : taskQueue) {
                if (holder.taskKey == id) {
                    taskQueue.remove(holder);
                    foundedHolder = holder;
                    hasTask = true;
                    break;
                }
            }
            executingTasks.remove(id);
        }

        if (hasTask) {
            onTaskEnd(id, foundedHolder.task);
        }
    }

    public boolean isExecuting(long id) {
        synchronized (taskQueue) {
            return executingTasks.contains(id);
        }
    }

    protected abstract void doTask(T task);

    protected abstract void onTaskStart(long id, T Task);

    protected abstract void onTaskEnd(long id, T Task);

    private TaskHolder findTask() {
        while (!isDestroyed) {
            synchronized (taskQueue) {
                if (taskQueue.size() > 0) {
                    return taskQueue.remove(0);
                }
                try {
                    taskQueue.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean hasTask(long key) {
        synchronized (taskQueue) {
            for (TaskHolder holder : taskQueue) {
                if (holder.taskKey == key) {
                    return true;
                }
            }
        }

        return false;
    }

    private class TaskThread extends Thread {
        @Override
        public void run() {
            while (!isDestroyed) {
                TaskHolder holder = findTask();
                if (holder == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                    continue;
                }

                try {
                    doTask(holder.task);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (!hasTask(holder.taskKey)) {
                    executingTasks.remove(holder.taskKey);
                    onTaskEnd(holder.taskKey, holder.task);
                }
            }
        }
    }

    private class TaskHolder {
        private long taskKey;
        private T task;

        private TaskHolder(long taskKey, T task) {
            this.taskKey = taskKey;
            this.task = task;
        }
    }
}
