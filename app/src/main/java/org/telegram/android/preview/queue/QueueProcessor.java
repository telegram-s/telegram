package org.telegram.android.preview.queue;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ex3ndr on 05.02.14.
 */
public class QueueProcessor<T extends QueueProcessor.BaseTask> {

    protected interface TaskFilter<T extends QueueProcessor.BaseTask> {
        public boolean isAccepted(T task);
    }

    private final ArrayList<TaskHolder> taskHolders = new ArrayList<TaskHolder>();
    private final HashMap<String, TaskHolder> taskMap = new HashMap<String, TaskHolder>();
    private boolean isRunning = true;

    public boolean isRunning() {
        return isRunning;
    }

    public void requestTask(T task) {
        synchronized (taskHolders) {
            TaskHolder holder = taskMap.get(task.getKey());
            if (holder != null) {
                synchronized (holder) {
                    if (holder.isRemoved) {
                        holder.isRemoved = false;
                    }
                }
                taskHolders.remove(holder);
                taskHolders.add(0, holder);
                taskHolders.notifyAll();
                return;
            }

            holder = new TaskHolder(task);
            taskHolders.add(0, holder);
            taskMap.put(task.getKey(), holder);
            taskHolders.notifyAll();
        }
    }

    public void removeTask(String taskKey) {
        synchronized (taskHolders) {
            TaskHolder holder = taskMap.get(taskKey);
            if (holder != null) {
                synchronized (holder) {
                    if (holder.isAcquired) {
                        holder.isRemoved = true;
                    } else {
                        taskMap.remove(taskKey);
                        taskHolders.remove(holder);
                    }
                }
            }
            taskHolders.notifyAll();
        }
    }

    public void taskCompleted(T task) {
        synchronized (taskHolders) {
            TaskHolder holder = taskMap.get(task.getKey());
            if (holder != null) {
                synchronized (holder) {
                    if (holder.isAcquired) {
                        taskMap.remove(task.getKey());
                        taskHolders.remove(holder);
                    }
                }
            }
            taskHolders.notifyAll();
        }
    }

    public T waitForTask(TaskFilter<T> taskFilter) {
        synchronized (taskHolders) {
            while (isRunning) {
                for (TaskHolder holder : taskHolders) {
                    synchronized (holder) {
                        if (holder.isAcquired) {
                            continue;
                        }
                        if (holder.isRemoved) {
                            continue;
                        }
                        if (taskFilter == null || taskFilter.isAccepted(holder.task)) {
                            holder.isAcquired = true;
                            return holder.task;
                        }
                    }
                }
                try {
                    taskHolders.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public void returnTaskToQueue(T task) {
        synchronized (taskHolders) {
            TaskHolder holder = taskMap.get(task.getKey());
            if (holder != null) {
                synchronized (holder) {
                    if (holder.isRemoved) {
                        taskMap.remove(task.getKey());
                        taskHolders.remove(holder);
                        return;
                    }
                    if (holder.isAcquired) {
                        holder.isAcquired = false;
                    }
                }
            }
            taskHolders.notifyAll();
        }
    }

    private class TaskHolder {
        private TaskHolder(T task) {
            this.task = task;
            this.isAcquired = false;
            this.isRemoved = false;
        }

        public T task;
        public boolean isAcquired;
        public boolean isRemoved;
    }

    public abstract static class BaseTask {
        public abstract String getKey();
    }
}
