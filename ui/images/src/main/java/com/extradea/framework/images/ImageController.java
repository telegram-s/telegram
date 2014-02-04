/*
 * Copyright (c) 2013 Extradea LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.extradea.framework.images;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.os.Process;
import android.util.Log;
import com.extradea.framework.images.cache.*;
import com.extradea.framework.images.tasks.*;
import com.extradea.framework.images.utils.ImageUtils;
import com.extradea.framework.images.workers.ImageWorker;
import com.extradea.framework.images.workers.WorkerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 01.06.12
 * Time: 16:53
 */

public class ImageController {

    private static final String TAG = "EX_IMAGE";

    private static final long TASK_DELAY = 0;// 1500;

    private static final int PAUSE_DELAY = 1500;

    private final ArrayList<TaskHolder> tasks = new ArrayList<TaskHolder>();
    private CopyOnWriteArrayList<WeakReference<ImageControllerListener>> listeners = new CopyOnWriteArrayList<WeakReference<ImageControllerListener>>();
    private CopyOnWriteArrayList<WeakReference<ImageTaskListener>> imageTaskCallbacks = new CopyOnWriteArrayList<WeakReference<ImageTaskListener>>();
    private final HashMap<String, TaskEvent> tasksStates = new HashMap<String, TaskEvent>();
    private final HashMap<String, ArrayList<TaskHolder>> tasksWithRequirements = new HashMap<String, ArrayList<TaskHolder>>();
    private ImagePersistence imagePersistence;
    private BitmapCache cache;

    private VerifierThread verifierThread;

    private CustomTaskWorkerThread[] workerThreads;
    private FixerThread fixerThread;

    private ReferenceCounter referenceCounter;

    public long lastPauseEvent;

    private boolean dynamicThreading = false;

    private int lastPriority = Thread.MIN_PRIORITY;

    private BitmapDecoder bitmapDecoder;

    public ImageController(Context context) {
        this(context, new ContextImagePersistence(context));
    }

    public ImageController(Context context, ImagePersistence imagePersistence) {
        this(context, imagePersistence, WorkerFactory.createDefaultWorkers(context));
    }

    public ImageController(Context context, ImageWorker[] workers) {
        this(context, new ContextImagePersistence(context), workers);
    }

    public ImageController(Context context, ImagePersistence imagePersistence, ImageWorker[] workers) {
        this.imagePersistence = imagePersistence;

        this.bitmapDecoder = new BitmapDecoder(this);

        referenceCounter = new ReferenceCounter(this);

        cache = BitmapCacheFactory.createMemoryCache(context);

        workerThreads = new CustomTaskWorkerThread[workers.length];
        for (int i = 0; i < workerThreads.length; i++) {
            workerThreads[i] = new CustomTaskWorkerThread(workers[i]);
            workerThreads[i].start();
        }

        verifierThread = new VerifierThread();
        verifierThread.start();

        fixerThread = new FixerThread();
        fixerThread.start();
    }

    public BitmapDecoder getBitmapDecoder() {
        return bitmapDecoder;
    }

    public void setThreadsPriority(int priority) {

        if (lastPriority == priority)
            return;

        lastPriority = priority;

        for (int i = 0; i < workerThreads.length; i++) {
            workerThreads[i].setPriority(priority);
        }

        verifierThread.setPriority(priority);
        fixerThread.setPriority(priority);
    }

    public void enableDynamicThreading() {
        dynamicThreading = true;
    }

    public void disableDynamicThreading() {
        dynamicThreading = false;
    }

    public boolean isDynamicThreading() {
        return dynamicThreading;
    }

    public ReferenceCounter getReferenceCounter() {
        return referenceCounter;
    }

    public void waitPaused() {
        synchronized (tasks) {
            if (getTime() - lastPauseEvent > PAUSE_DELAY) {
                return;
            }

            try {
                tasks.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public void doResume() {
        lastPauseEvent = 0;
        fixerThread.fixDelayed(0);
        onResume();
    }

    public void doPause(int delay) {
        lastPauseEvent = getTime() - (PAUSE_DELAY - delay);
        fixerThread.fixDelayed(delay);
        onPause();
    }

    public void doPause() {
        doPause(PAUSE_DELAY);
    }

    private void onPause() {
        if (dynamicThreading) {
            setThreadsPriority(Thread.MIN_PRIORITY);
        }
    }

    private void onResume() {
        if (dynamicThreading) {
            setThreadsPriority(Thread.NORM_PRIORITY);
        }
    }

    private long getTime() {
        return android.os.SystemClock.uptimeMillis();
    }

    public void clearCache() {
        cache.clear();
    }

    public void removeFromCache(String key) {
        cache.removeFromCache(key);
    }

    public void addImageTaskListeners(ImageTaskListener listener) {
        for (WeakReference<ImageTaskListener> l : imageTaskCallbacks) {
            if (l.get() == listener)
                return;
        }
        imageTaskCallbacks.add(new WeakReference<ImageTaskListener>(listener));
    }

    public void removeImageTaskListener(ImageTaskListener listener) {
        for (WeakReference<ImageTaskListener> l : imageTaskCallbacks) {
            if (l.get() == listener) {
                imageTaskCallbacks.remove(l);
                return;
            }
        }
    }

    public void addListener(ImageControllerListener listener) {
        for (WeakReference<ImageControllerListener> l : listeners) {
            if (l.get() == listener)
                return;
        }
        listeners.add(new WeakReference<ImageControllerListener>(listener));
    }

    public void removeListener(ImageControllerListener listener) {
        for (WeakReference<ImageControllerListener> l : listeners) {
            if (l.get() == listener) {
                listeners.remove(l);
                return;
            }
        }
    }

    private Bitmap findInCache(ImageTask task) {
        return cache.take(task.getKey());
    }

    public void removeTask(ImageTask task) {

        synchronized (tasks) {
            TaskHolder holder = null;
            for (TaskHolder t : tasks) {
                if (t.task.getKey().equals(task.getKey())) {
                    holder = t;
                    break;
                }
            }
            if (holder != null) {
                if (holder.decRequests() == 0) {
                    tasks.remove(holder);
                    notifyTaskState(task, TaskEvent.EVENT_REMOVED);
                } else {
                }
            } else {
            }

            if (task.getRequiredTasks() != null) {
                for (ImageTask reqTask : task.getRequiredTasks()) {
                    removeTask(reqTask);
                }
            }

            tasks.notifyAll();
        }

    }

    public Bitmap tryToFindInCache(ImageTask task) {
        Bitmap cached = findInCache(task);
        if (cached != null) {
            return cached;
        }

        return null;
    }

    public Bitmap tryToFindInPersistence(ImageTask task) {
        Bitmap cached = findInCache(task);
        if (cached != null) {
            return cached;
        }

        Bitmap res = imagePersistence.loadImage(task.getKey());
        if (res != null) {
            return res;
        }

        return null;
    }

    public Bitmap addTaskFS(ImageTask task) {

        Bitmap cached = findInCache(task);
        if (cached != null) {
            return cached;
        }

        Bitmap res = imagePersistence.loadImage(task.getKey());
        if (res != null) {
            return res;
        }
        return addTask(task);
    }

    public Bitmap addTask(ImageTask task) {
        Bitmap cached = findInCache(task);
        if (cached != null) {
            notifyTaskState(task, TaskEvent.EVENT_FINISHED);
            return cached;
        }

        synchronized (tasks) {
            synchronized (tasksWithRequirements) {
                Stack<ImageTask> taskStack = new Stack<ImageTask>();
                taskStack.push(task);

                while (!taskStack.empty()) {
                    ImageTask currentTask = taskStack.pop();

                    TaskHolder holder = null;
                    for (TaskHolder t : tasks) {
                        if (t.task.getKey().equals(currentTask.getKey())) {
                            holder = t;
                            break;
                        }
                    }
                    if (holder != null) {
                        tasks.remove(holder);
                        holder.setAddTime(getTime());
                        holder.incRequests();
                        tasks.add(0, holder);
                    } else {
                        holder = new TaskHolder(false, currentTask, getTime());
                        if (currentTask.skipDiskCacheCheck()) {
                            holder.setVerified(true);
                        }
                        tasks.add(0, holder);
                        notifyTaskState(currentTask, TaskEvent.EVENT_CREATED);
                    }

                    if (currentTask.getRequiredTasks() != null) {
                        for (ImageTask reqTask : currentTask.getRequiredTasks()) {
                            if (!taskStack.contains(reqTask)) {
                                taskStack.push(reqTask);
                            }
                            if (tasksWithRequirements.containsKey(reqTask.getKey())) {
                                tasksWithRequirements.get(reqTask.getKey()).add(holder);
                            } else {
                                tasksWithRequirements.put(reqTask.getKey(), new ArrayList<TaskHolder>());
                                tasksWithRequirements.get(reqTask.getKey()).add(holder);
                            }
                        }
                    }
                }
            }
            tasks.notifyAll();
        }

        return null;
    }

    // FIX
    private Bitmap addLowPriorityTask(ImageTask task) {

        Bitmap cached = findInCache(task);
        if (cached != null) {
            return cached;
        }

        synchronized (tasks) {
            for (TaskHolder t : tasks) {
                if (t.getOriginalKey().equals(task.getKey())) {
                    return null;
                }
            }
            tasks.add(new TaskHolder(false, task, getTime()));
            tasks.notifyAll();
        }
        return null;
    }

    private TaskHolder waitForTask(TaskFilter filter, boolean pausable) {
        TaskHolder result = null;
        while (result == null) {
            result = takeTask(filter, pausable);
            if (result == null) {
                synchronized (tasks) {
                    try {
                        tasks.wait();
                    } catch (InterruptedException e) {
                        return null;
                    }
                }
            }
        }
        notifyTaskState(result.task, TaskEvent.EVENT_STARTED);
        return result;
    }

    private TaskHolder takeTask(TaskFilter filter, boolean pausable) {
        if (getTime() - lastPauseEvent < PAUSE_DELAY && pausable) {
            return null;
        }

        TaskHolder resultTask = null;
        synchronized (tasks) {
            for (TaskHolder task : tasks) {
                if (task.isVerified()
                        && (!task.isInProgress())
                        && (!task.isWaitingForRequirements())
                        && (getTime() - task.getAddTime() > TASK_DELAY)) {

                    if (filter.acceptTask(task.getTask())) {
                        resultTask = task;
                        break;
                    }
                }
            }

            if (resultTask != null) {
                resultTask.setInProgress(true);
            }
        }

        return resultTask;
    }

    private TaskHolder takeUnverifiedTask() {
        if (getTime() - lastPauseEvent < PAUSE_DELAY) {
            return null;
        }

        TaskHolder resultTask = null;
        synchronized (tasks) {
            for (TaskHolder task : tasks) {
                if (!task.isVerified() && (getTime() - task.getAddTime() > TASK_DELAY)) {
                    resultTask = task;
                    break;
                }
            }
        }

        return resultTask;
    }

    private void onTaskVerified(TaskHolder holder) {
        synchronized (tasks) {
            holder.setVerified(true);
            tasks.notifyAll();
        }
    }

    private void notifyTaskState(ImageTask task, int id) {
        notifyTaskState(task, id, null);
    }

    public void notifyTaskState(ImageTask task, int id, Object[] args) {
        long start = SystemClock.uptimeMillis();
        TaskEvent event;
        synchronized (tasksStates) {
            if (tasksStates.containsKey(task.getKey())) {
                event = tasksStates.get(task.getKey());
                event.setArgs(args);
                event.setEventId(id);
            } else {
                event = new TaskEvent(task.getKey(), id, args);
                tasksStates.put(task.getKey(), event);
            }
        }

        for (WeakReference<ImageTaskListener> listener : imageTaskCallbacks) {
            ImageTaskListener l = listener.get();
            if (l == null) {
                imageTaskCallbacks.remove(listener);
            } else {
                l.onTaskEvent(task, id, args);
            }
        }
        // Log.d(TAG, "notify in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public TaskEvent getLastTaskEvent(String key) {
        synchronized (tasksStates) {
            if (tasksStates.containsKey(key)) {
                return tasksStates.get(key);

            }
        }
        return null;
    }

    private void notifyOnTaskFinished(ImageTask task) {
        for (WeakReference<ImageControllerListener> listener : listeners) {
            ImageControllerListener l = listener.get();
            if (l != null) {
                l.onTaskFinished(task);
            } else {
                listeners.remove(l);
            }
        }
    }

    private void notifyOnTaskFailed(ImageTask task) {
        for (WeakReference<ImageControllerListener> listener : listeners) {
            ImageControllerListener l = listener.get();
            if (l != null) {
                l.onTaskFailed(task);
            } else {
                listeners.remove(l);
            }
        }
    }

    private void onTaskFinished(ImageTask task, boolean alreadyDone) {
        if (task.getRequiredTasks() != null) {
            for (ImageTask t : task.getRequiredTasks()) {
                t.setResult(null);
            }
        }

        if (task.isPutInDiskCache() && !alreadyDone) {
            if (task.getBinaryResult() != null) {
                imagePersistence.saveImage(task.getBinaryResult(), task.getKey());
            } else {
                if (task.isForceSaveToFS()) {
                    imagePersistence.saveImage(task.getResult(), task.getKey());
                }
            }
        }

        if (task.isPutInMemoryCache()) {
            cache.cache(task.getKey(), task.getResult());
        }
        synchronized (tasksWithRequirements) {
            if (tasksWithRequirements.containsKey(task.getKey())) {
                for (TaskHolder t : tasksWithRequirements.get(task.getKey())) {
                    for (int i = 0; i < t.task.getRequiredTasks().length; i++) {
                        if (t.task.getRequiredTasks()[i].getKey().equals(task.getKey())) {
                            t.satisfiesRequirement[i] = true;
                            t.task.getRequiredTasks()[i].setResult(task.getResult());
                        }
                    }
                }
                tasksWithRequirements.remove(task.getKey());
            }
        }
        synchronized (tasks) {
            tasks.notifyAll();
        }

        notifyTaskState(task, TaskEvent.EVENT_FINISHED);
        notifyOnTaskFinished(task);
    }

    private void onTaskFailed(ImageTask task) {
        ArrayList<ImageTask> dependency = new ArrayList<ImageTask>();
        synchronized (tasksWithRequirements) {
            if (tasksWithRequirements.containsKey(task.getKey())) {
                for (TaskHolder t : tasksWithRequirements.get(task.getKey())) {
                    for (int i = 0; i < t.task.getRequiredTasks().length; i++) {
                        if (t.task.getRequiredTasks()[i].getKey().equals(task.getKey())) {
                            dependency.add(t.task);

                        }
                    }
                }

                tasksWithRequirements.remove(task.getKey());
            }
        }

        for (ImageTask dep : dependency) {
            removeTask(dep);
        }

        for (ImageTask dep : dependency) {
            onTaskFailed(dep);
        }

        notifyTaskState(task, TaskEvent.EVENT_FINISHED_WITH_ERROR);
        notifyOnTaskFailed(task);
    }

    private class FixerThread extends Thread {

        private Handler handler;

        public FixerThread() {
            setName("FixerThread_" + hashCode());
        }

        public void fixDelayed(int delay) {
            if (handler != null) {
                handler.removeCallbacks(null);
                handler.sendEmptyMessageDelayed(0, delay);
            }
        }

        @Override
        public void run() {

            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

            Looper.prepare();

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    synchronized (tasks) {
                        if (getTime() - lastPauseEvent < PAUSE_DELAY) {

                            handler.removeCallbacks(null);
                            handler.sendEmptyMessageDelayed(0, 100);
                        } else {
                            onResume();
                            tasks.notifyAll();
                        }
                    }
                }
            };

            Looper.loop();
        }
    }

    private class VerifierThread extends Thread {

        private boolean verifyTask(final ImageTask task) {
            Thread.yield();

            Bitmap img = getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
                @Override
                public Bitmap call() throws Exception {
                    return imagePersistence.loadImageOptimized(task.getKey());
                }
            });
            Thread.yield();
            if (img != null) {
                task.setResult(img);
                return false;
            } else {
                return true;
            }
        }

        public VerifierThread() {
            setName("VerifierThread_" + hashCode());
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            while (!isInterrupted()) {
                try {
                    TaskHolder holder = null;
                    while (holder == null) {
                        Thread.sleep(20);
                        holder = takeUnverifiedTask();
                        if (holder == null) {
                            synchronized (tasks) {
                                try {
                                    tasks.wait();
                                } catch (InterruptedException e) {
                                    return;
                                }
                            }
                        }
                    }

                    if (!verifyTask(holder.task)) {
                        synchronized (tasks) {
                            tasks.remove(holder);
                        }
                        onTaskFinished(holder.task, true);
                    } else {
                        onTaskVerified(holder);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private abstract class ImageTaskWorker extends Thread implements TaskFilter {

        protected static final int RESULT_OK = ImageWorker.RESULT_OK;
        protected static final int RESULT_FAILURE = ImageWorker.RESULT_FAILURE;
        protected static final int RESULT_REPEAT = ImageWorker.RESULT_REPEAT;

        public abstract boolean acceptTask(ImageTask task);

        protected abstract int processTask(ImageTask task);

        protected abstract void safeWaiting();

        protected abstract boolean isPausable();

        public ImageTaskWorker() {
            setName(getClass().getSimpleName() + "_" + hashCode());
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            while (!isInterrupted()) {
                //Log.d(TAG, "Worker (" + this + ") waiting for task");
                TaskHolder holder = null;
                int result = RESULT_FAILURE;
                try {
                    holder = waitForTask(this, isPausable());
                    if (holder != null) {
                        result = processTask(holder.task);
                        switch (result) {
                            case RESULT_OK:
                                onTaskFinished(holder.task, false);
                                break;
                            case RESULT_FAILURE:
                                onTaskFailed(holder.task);
                                break;
                            case RESULT_REPEAT:
                            default:
                                addLowPriorityTask(holder.task);
                                break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    onTaskFailed(holder.task);
                } finally {
                    if (result != RESULT_REPEAT) {
                        synchronized (tasks) {
                            tasks.remove(holder);
                        }
                    }
                }
                safeWaiting();
            }
        }
    }

    private class CustomTaskWorkerThread extends ImageTaskWorker {
        private ImageWorker worker;

        public CustomTaskWorkerThread(ImageWorker worker) {
            this.worker = worker;
            setName(worker.getClass().getSimpleName() + "#" + hashCode());
        }

        @Override
        public boolean acceptTask(ImageTask task) {
            return worker.acceptTask(task, ImageController.this);
        }

        @Override
        protected int processTask(ImageTask task) {
            return worker.processTask(task, ImageController.this);
        }

        @Override
        protected void safeWaiting() {
        }

        @Override
        protected boolean isPausable() {
            return worker.isPausable();
        }
    }

    private interface TaskFilter {
        abstract boolean acceptTask(ImageTask task);
    }

    private class TaskHolder {
        private boolean[] satisfiesRequirement;
        private boolean isInProgress;
        private boolean verified;
        private ImageTask task;
        private String originalKey;
        private long addTime;
        private int requestsCount;

        private TaskHolder(boolean verified, ImageTask task, long time) {
            this.verified = verified;
            this.task = task;
            this.originalKey = task.getKey();
            this.addTime = time;
            this.requestsCount = 1;

            if (task.getRequiredTasks() != null) {
                satisfiesRequirement = new boolean[task.getRequiredTasks().length];
                for (int i = 0; i < satisfiesRequirement.length; i++) {
                    satisfiesRequirement[i] = false;
                }
            }
        }

        public boolean isVerified() {
            return verified;
        }

        public ImageTask getTask() {
            return task;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public void setTask(ImageTask task) {
            this.task = task;
        }

        public String getOriginalKey() {
            return originalKey;
        }

        public boolean isInProgress() {
            return isInProgress;
        }

        public void setInProgress(boolean inProgress) {
            isInProgress = inProgress;
        }

        public boolean isWaitingForRequirements() {
            if (satisfiesRequirement != null) {
                boolean res = true;
                for (boolean b : satisfiesRequirement) {
                    res &= b;
                }
                return !res;
            } else {
                return false;
            }
        }

        public boolean[] getSatisfiesRequirement() {
            return satisfiesRequirement;
        }

        public long getAddTime() {
            return addTime;
        }

        public void setAddTime(long addTime) {
            this.addTime = addTime;
        }

        public int incRequests() {
            return ++requestsCount;
        }

        public int decRequests() {
            return --requestsCount;
        }

        public int getRequestsCount() {
            return requestsCount;
        }

        public void setRequestsCount(int requestsCount) {
            this.requestsCount = requestsCount;
        }

        @Override
        public String toString() {
            return "[Holder: " + task + " ]";
        }
    }

    public interface ImageControllerListener {

        public void onTaskFinished(ImageTask task);

        public void onTaskFailed(ImageTask task);
    }
}