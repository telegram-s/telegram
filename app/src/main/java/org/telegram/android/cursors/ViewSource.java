package org.telegram.android.cursors;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.UiResponsibility;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 04.08.13 7:50
 */
public abstract class ViewSource<T> {
    private ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("ViewSourceService#" + res.hashCode());
            return res;
        }
    });

    private final String TAG;

    private HashMap<Long, T> items;
    // private TreeSet<T> items;
    private ArrayList<T> workingSet;
    private ArrayList<T> nextWorkingSet;

    private InternalSourceState state;

    private Comparator<T> comparator;

    private CopyOnWriteArrayList<ViewSourceListener> listeners = new CopyOnWriteArrayList<ViewSourceListener>();

    private boolean invalidated = false;

    private boolean isDestroyed = false;

    private UiResponsibility responsibility;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                for (ViewSourceListener listener : listeners) {
                    listener.onSourceStateChanged();
                }
            } else if (msg.what == 1) {
                workingSet = nextWorkingSet;
                for (ViewSourceListener listener : listeners) {
                    listener.onSourceDataChanged();
                }
            } else if (msg.what == 2) {
                workingSet = nextWorkingSet;
                for (ViewSourceListener listener : listeners) {
                    listener.onSourceDataChanged();
                }
                for (ViewSourceListener listener : listeners) {
                    listener.onSourceStateChanged();
                }
            }
        }
    };

    public ViewSource() {
        this(null);
    }

    public ViewSource(UiResponsibility responsibility) {
        TAG = "ViewSource:" + getClass().getSimpleName() + "#" + hashCode();
        this.responsibility = responsibility;
        items = new HashMap<Long, T>();
        comparator = new Comparator<T>() {
            @Override
            public int compare(T t, T t2) {
                long key1 = getSortingKey(t2);
                long key2 = getSortingKey(t);
                if (key1 == key2) {
                    return 0;
                }
                if (key1 > key2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        };
        workingSet = new ArrayList<T>();
        state = InternalSourceState.UNSYNCED;
    }

    public ArrayList<T> getCurrentWorkingSet() {
        return workingSet;
    }


    public void onConnected() {
        if (state == InternalSourceState.UNSYNCED) {
            requestLoad();
        }
    }

    private synchronized ArrayList<T> buildNewWorkingSet() {
        Logger.d(TAG, "buildNewWorkingSet");
        long start = SystemClock.uptimeMillis();
        ArrayList<T> res = new ArrayList<T>(items.values());
        Collections.sort(res, comparator);
        Logger.d(TAG, "buildNewWorkingSet in " + (SystemClock.uptimeMillis() - start) + " ms");
        return res;
    }

    public void addListener(ViewSourceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ViewSourceListener listener) {
        listeners.remove(listener);
    }

    private synchronized void setState(InternalSourceState state) {
        Logger.d(TAG, "setState");
        this.state = state;
        invalidateState();
        Logger.d(TAG, "setState end");
    }

    public synchronized void invalidateData() {
        Logger.d(TAG, "invalidateData");
        nextWorkingSet = buildNewWorkingSet();
        if (responsibility != null) {
            responsibility.waitForResume();
        }
        handler.removeMessages(1);
        handler.sendEmptyMessage(1);
        Logger.d(TAG, "invalidateData end");
    }

    private synchronized void invalidateDataAndState(InternalSourceState state) {
        this.state = state;
        nextWorkingSet = buildNewWorkingSet();
        if (responsibility != null) {
            responsibility.waitForResume();
        }
        handler.removeMessages(0);
        handler.removeMessages(1);
        handler.removeMessages(2);
        handler.sendEmptyMessage(2);
    }

    public synchronized void invalidateDataIfRequired() {
        if (invalidated) {
            invalidated = false;
            invalidateData();
        }
    }

    public synchronized void invalidateState() {
        handler.removeMessages(0);
        handler.sendEmptyMessage(0);
    }

    private void requestLoad() {
        if (state == InternalSourceState.IN_PROGRESS ||
                state == InternalSourceState.COMPLETED) {
            return;
        }
        setState(InternalSourceState.IN_PROGRESS);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                long start = SystemClock.uptimeMillis();
                int offset = items.size();
                T[] res = loadItems(offset);
                Logger.w(TAG, "loadingMore loaded " + res.length + " in " + (SystemClock.uptimeMillis() - start) + " ms");
                boolean hasAdded = false;
                for (T re : res) {
                    if (!addItem(re)) {
                        hasAdded = true;
                    }
                }
                if (!hasAdded) {
                    Logger.w(TAG, "Completed loading");
                    invalidateDataAndState(InternalSourceState.COMPLETED);
                } else {
                    invalidateDataAndState(InternalSourceState.SYNCED);
                }
            }
        });
    }

    public int getItemsCount() {
        return workingSet.size();
    }

    public T getItem(int index) {
        if (state == InternalSourceState.COMPLETED) {
            onItemRequested(index);
        } else {
            if (index > items.size() - 10) {
                requestLoad();
            }
        }
        return workingSet.get(index);
    }

    public synchronized boolean addItem(T itm) {
        long key = getItemKey(itm);
        boolean res = items.containsKey(key);
        items.put(key, itm);
        invalidated = true;
        return res;
    }

    public synchronized boolean addToEndHacky(T itm) {
        long key = getItemKey(itm);
        boolean res = items.containsKey(key);
        items.put(key, itm);
        workingSet.add(0, itm);
        invalidated = true;
        return res;
    }

    public synchronized void removeItem(T itm) {
        items.remove(getItemKey(itm));
        invalidated = true;
    }

    public synchronized void updateItem(T itm) {
        Logger.d(TAG, "updateItem: " + itm);
        long key = getItemKey(itm);
        Logger.d(TAG, "item key: " + key);
//        if (!items.containsKey(key)) {
//            return;
//        }
        items.put(key, itm);
        invalidated = true;
    }

    protected abstract T[] loadItems(int offset);

    protected abstract long getSortingKey(T obj);

    protected abstract long getItemKey(T obj);

    protected abstract ViewSourceState getInternalState();

    public ViewSourceState getState() {
        if (state == InternalSourceState.UNSYNCED) {
            return ViewSourceState.UNSYNCED;
        } else if (state == InternalSourceState.SYNCED) {
            return ViewSourceState.SYNCED;
        } else if (state == InternalSourceState.IN_PROGRESS) {
            return ViewSourceState.IN_PROGRESS;
        } else {
            return getInternalState();
        }
    }

    public void onItemsShown(int index) {
        if (state == InternalSourceState.COMPLETED) {
            onItemRequested(index);
        } else {
            if (index > items.size() - 10) {
                requestLoad();
            }
        }
    }

    protected void onItemRequested(int index) {

    }

    private static enum InternalSourceState {
        UNSYNCED, IN_PROGRESS, SYNCED, COMPLETED
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void destroy() {
        isDestroyed = true;
    }
}