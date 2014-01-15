package org.telegram.android.ui.source;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
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
public abstract class ViewSource<T, V> {
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
            Logger.d(TAG, "notify");
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
        this(false);
    }

    public ViewSource(boolean preload) {
        this(null, preload);
    }

    public ViewSource(UiResponsibility responsibility, boolean preload) {
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
        if (preload) {
            if (!loadMore()) {
                state = InternalSourceState.COMPLETED;
            } else {
                state = InternalSourceState.SYNCED;
            }
            workingSet = buildNewWorkingSet();
        } else {
            workingSet = new ArrayList<T>();
            state = InternalSourceState.UNSYNCED;
        }
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
        long start = SystemClock.uptimeMillis();
        ArrayList<T> res = new ArrayList<T>(items.values());
        Collections.sort(res, comparator);
        Logger.d(TAG, "Building new working set in " + (SystemClock.uptimeMillis() - start) + " ms");
        return res;
    }

    public void addListener(ViewSourceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ViewSourceListener listener) {
        listeners.remove(listener);
    }

    private synchronized void setState(InternalSourceState state) {
        this.state = state;
        invalidateState();
    }

    public synchronized void invalidateData() {
        Logger.d(TAG, "invalidateData");
        nextWorkingSet = buildNewWorkingSet();
        if (responsibility != null) {
            responsibility.waitForResume();
        }
        handler.removeMessages(1);
        handler.sendEmptyMessage(1);
    }

    private synchronized void invalidateDataAndState(InternalSourceState state) {
        Logger.d(TAG, "invalidateDataAndState");
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
        Logger.d(TAG, "invalidateDataIfRequired");
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
                if (!loadMore()) {
                    Logger.w(TAG, "Completed loading");
                    invalidateDataAndState(InternalSourceState.COMPLETED);
                } else {
                    invalidateDataAndState(InternalSourceState.SYNCED);
                }
            }
        });
    }

    private boolean loadMore() {
        long start = SystemClock.uptimeMillis();
        int offset = items.size();
        V[] res = loadItems(offset);
        Logger.w(TAG, "loadingMore loaded " + res.length + " in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        boolean hasAdded = addItems(res);
        Logger.w(TAG, "loadingMore added " + res.length + " in " + (SystemClock.uptimeMillis() - start) + " ms");
        return hasAdded;
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

    public synchronized boolean addItem(V itm) {
        T dest = convert(itm);
        long key = getItemKey(dest);
        boolean res = items.containsKey(key);
        items.put(key, dest);
        invalidated = true;
        return res;
    }

    public synchronized boolean addItems(V... itm) {
        long start = SystemClock.uptimeMillis();
        Object[] converted = new Object[itm.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = convert(itm[i]);
        }
        Logger.d(TAG, "Converted in " + (SystemClock.uptimeMillis() - start) + " ms");

        boolean added = false;
        for (int i = 0; i < converted.length; i++) {
            T dest = (T) converted[i];
            long key = getItemKey(dest);
            added = !items.containsKey(key) | added;
            items.put(key, dest);
        }
        invalidated = true;
        return added;
    }

    public synchronized boolean addToEndHacky(V itm) {
        T dest = convert(itm);
        long key = getItemKey(dest);
        boolean added = !items.containsKey(key);
        items.put(key, dest);
        workingSet.add(0, dest);
        invalidated = true;
        return added;
    }

    public synchronized void removeItem(V itm) {
        items.remove(getItemKeyV(itm));
        invalidated = true;
    }

    public synchronized void removeItemByKey(long itm) {
        items.remove(itm);
        invalidated = true;
    }

    public synchronized void updateItem(V itm) {
        T dest = convert(itm);
        // Logger.d(TAG, "updateItem: " + itm);
        long key = getItemKey(dest);
        // Logger.d(TAG, "item key: " + key);
//        if (!items.containsKey(key)) {
//            return;
//        }
        items.put(key, dest);
        invalidated = true;
    }

    public synchronized void updateItems(V... itm) {
        long start = SystemClock.uptimeMillis();
        Object[] converted = new Object[itm.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = convert(itm[i]);
        }
        Logger.d(TAG, "Converted in " + (SystemClock.uptimeMillis() - start) + " ms");

        for (int i = 0; i < converted.length; i++) {
            T dest = (T) converted[i];
            long key = getItemKey(dest);
            items.put(key, dest);
        }
        invalidated = true;
    }

    protected abstract V[] loadItems(int offset);

    protected abstract long getSortingKey(T obj);

    protected abstract long getItemKey(T obj);

    protected abstract long getItemKeyV(V obj);

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

    protected abstract T convert(V item);
}