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
public abstract class BaseSync {
    private static final String PREFERENCES_NAME = "org.telegram.android";
    private final String TAG;

    private static final long ERROR_DELAY = 5000;
    private static final long NO_API_DELAY = 10000;
    private static final long NO_LOGIN_DELAY = 10000;

    private static final int HANDLER_ONLINE = 0;
    private static final int HANDLER_OFFLINE = 1;

    private HashMap<Integer, SyncHolder> syncEntities = new HashMap<Integer, SyncHolder>();

    private HashMap<Integer, Handler> syncHandlers;

    private HandlerThread networkThread;
    private HandlerThread offlineThread;
    private Handler networkHandler;
    private Handler offlineHandler;

    private StelsApplication application;

    protected SharedPreferences preferences;

    protected BaseSync(StelsApplication application) {
        this.TAG = getClass().getName();
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

        this.syncHandlers = new HashMap<Integer, Handler>();
        this.syncHandlers.put(HANDLER_ONLINE, networkHandler);
        this.syncHandlers.put(HANDLER_OFFLINE, offlineHandler);

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

    private Handler getHandler(SyncHolder holder) {
        if (holder.isOffline) {
            return syncHandlers.get(HANDLER_OFFLINE);
        } else {
            return syncHandlers.get(HANDLER_ONLINE);
        }
    }

    protected void registerOfflineSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, true, true);
    }

    protected void registerTechSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, false, false);
    }

    protected void registerSync(int id, String methodName, int timeout) {
        registerSync(id, methodName, timeout, true, true, true, false);
    }

    protected void registerSyncSingle(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, true, true, false);
    }

    protected void registerSyncSingleOffline(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, true, true, true);
    }

    protected void registerSyncEvent(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, false, true, false);
    }

    protected void registerSyncEventOffline(int id, String methodName) {
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
            entity.isInProgress = true;
        }
        try {
            Logger.d(TAG, "Starting sync: " + entity.name);
            entity.method.invoke(this);
            Logger.d(TAG, "Sync ended: " + entity.name);
            synchronized (entity) {
                entity.isInProgress = false;
                if (entity.isInvalidatedDuringExecution) {
                    return;
                }
                if (entity.isCyclic) {
                    markAsSynced(syncId);
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, "Sync failure: " + entity.name);
            Logger.t(TAG, e);
            synchronized (entity) {
                entity.isInProgress = false;
                entity.isInvalidatedDuringExecution = false;
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

        synchronized (entity) {
            if (entity.isInProgress) {
                entity.isInvalidatedDuringExecution = true;
            }
        }

        Handler handler = getHandler(entity);
        handler.removeMessages(entity.id);
        if (delay > 0) {
            handler.sendEmptyMessageDelayed(entity.id, delay);
        } else {
            handler.sendEmptyMessage(entity.id);
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
        public boolean isInProgress;
        public boolean isInvalidatedDuringExecution;
    }
}
