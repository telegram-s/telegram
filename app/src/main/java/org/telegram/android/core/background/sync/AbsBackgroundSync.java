package org.telegram.android.core.background.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Created by ex3ndr on 01.12.13.
 */
public abstract class AbsBackgroundSync {
    private static final String PREFERENCES_NAME = "org.telegram.android";
    private static final String TAG = "AbsBackgroundSync";

    private static final long ERROR_DELAY = 5000;
    private static final long NO_API_DELAY = 10000;
    private static final long NO_LOGIN_DELAY = 10000;

    private HashMap<Integer, SyncHolder> syncEntities = new HashMap<Integer, SyncHolder>();

    private HandlerThread networkThread;
    private HandlerThread offlineThread;
    private Handler networkHandler;
    private Handler offlineHandler;

    private StelsApplication application;

    protected SharedPreferences preferences;

    protected AbsBackgroundSync(StelsApplication application) {
        this.application = application;
        this.preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    protected void init() {
        this.networkThread = new HandlerThread("BackgroundNetworkSyncThread", Thread.MIN_PRIORITY);
        this.offlineThread = new HandlerThread("BackgroundOfflineSyncThread", Thread.MIN_PRIORITY);
        this.networkThread.start();
        this.offlineThread.start();

        while (this.networkThread.getLooper() == null) {
            Thread.yield();
        }
        while (this.offlineThread.getLooper() == null) {
            Thread.yield();
        }

        this.networkHandler = new SyncHandler(networkThread.getLooper());
        this.offlineHandler = new SyncHandler(offlineThread.getLooper());

        for (SyncHolder entity : syncEntities.values()) {
            if (!entity.isAutostart) {
                continue;
            }
            if (!entity.isCyclic) {
                requestSync(entity.id);
                continue;
            }

            long syncTime = this.preferences.getLong("background.sync_" + entity.id, 0);
            if (System.currentTimeMillis() < syncTime) {
                syncTime = 0;
            }
            long delta = Math.min(Math.max(0, entity.syncInterval * 1000L - (System.currentTimeMillis() - syncTime)),
                    entity.syncInterval * 1000L);
            requestSyncDelayed(entity.id, delta);
        }
    }

    protected void registerOfflineSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, true, true);
    }

    protected void registerTechSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, false, true);
    }

    protected void registerSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, true, true);
    }

    protected void registerSyncSingle(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, true, true, true);
    }

    protected void registerSyncEvent(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, false, true, true);
    }

    protected void registerSync(int id, String methodName, int timeout, boolean isCyclic, boolean isAutostart, boolean loginRequired, boolean isOffline) {
        SyncHolder holder = new SyncHolder();
        holder.id = id;
        holder.isOffline = isOffline;
        holder.syncInterval = timeout;
        holder.name = methodName;
        holder.loginRequired = loginRequired;
        holder.isCyclic = isCyclic;
        holder.isAutostart = isAutostart;
        try {
            holder.method = getClass().getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        syncEntities.put(id, holder);
        Logger.d(TAG, "Registered sync #" + id + " " + methodName);
    }

    protected void performSync(int syncId) {
        SyncHolder entity = syncEntities.get(syncId);
        if (entity == null) {
            Logger.w(TAG, "Ingnoring performSync #" + syncId);
            return;
        }

        if (!entity.isOffline && application.getApi() == null) {
            Logger.d(TAG, "API Not ready, delaying sync: " + entity.name);
            requestSyncDelayed(syncId, NO_API_DELAY);
            return;
        }

        if (entity.loginRequired && !application.isLoggedIn()) {
            Logger.d(TAG, "User not logged in, delaying sync: " + entity.name);
            requestSyncDelayed(syncId, NO_LOGIN_DELAY);
            return;
        }

        synchronized (entity) {
            try {
                Logger.d(TAG, "Starting sync: " + entity.name);
                entity.method.invoke(this);
                Logger.d(TAG, "Sync ended: " + entity.name);
                if (entity.isCyclic) {
                    markAsSynced(syncId);
                }
            } catch (Exception e) {
                Logger.w(TAG, "Sync failure: " + entity.name);
                Logger.t(TAG, e);
                syncFailure(syncId);
            }
        }
    }

    protected void resetSync(int syncId) {
        SyncHolder entity = syncEntities.get(syncId);
        if (entity == null) {
            return;
        }
        if (entity.isCyclic) {
            preferences.edit().putLong("background.sync_" + syncId, 0L);
        }
        requestSync(syncId);
    }

    protected void markAsSynced(int syncId) {
        SyncHolder entity = syncEntities.get(syncId);
        if (entity == null) {
            return;
        }
        if (!entity.isCyclic) {
            return;
        }
        preferences.edit().putLong("background.sync_" + syncId, System.currentTimeMillis());
        requestSyncDelayed(syncId, entity.syncInterval * 1000L);
    }

    protected void syncFailure(int syncId) {
        requestSyncDelayed(syncId, ERROR_DELAY);
    }

    private void requestSync(int syncId) {
        requestSyncDelayed(syncId, 0);
    }

    private void requestSyncDelayed(int syncId, long delay) {
        SyncHolder entity = syncEntities.get(syncId);
        if (entity == null) {
            return;
        }

        if (entity.isOffline) {
            offlineHandler.removeMessages(entity.id);
            if (delay > 0) {
                offlineHandler.sendEmptyMessageDelayed(entity.id, delay);
            } else {
                offlineHandler.sendEmptyMessage(entity.id);
            }
        } else {
            networkHandler.removeMessages(entity.id);
            if (delay > 0) {
                networkHandler.sendEmptyMessageDelayed(entity.id, delay);
            } else {
                networkHandler.sendEmptyMessage(entity.id);
            }
        }
    }

    private class SyncHandler extends Handler {
        private SyncHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            performSync(msg.what);
        }
    }

    private class SyncHolder {
        public int id;
        public String name;
        public boolean isOffline;
        public boolean loginRequired;
        public int syncInterval;
        public boolean isCyclic;
        public boolean isAutostart;
        public Method method;
    }
}
