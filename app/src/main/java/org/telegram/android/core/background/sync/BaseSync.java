package org.telegram.android.core.background.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by ex3ndr on 01.12.13.
 */
public abstract class BaseSync {
    private final String TAG;

    protected static final int SEC = 1;
    protected static final int MIN = 60 * SEC;
    protected static final int HOUR = 60 * MIN;

    private static final long ERROR_DELAY = 5000;
    private static final long NO_API_DELAY = 10000;
    private static final long NO_LOGIN_DELAY = 10000;

    private static final int HANDLER_ONLINE = 0;
    private static final int HANDLER_OFFLINE = 1;

    private SortedMap<Integer, SyncHolder> syncEntities = new TreeMap<Integer, SyncHolder>();

    private HashMap<Integer, Handler> syncHandlers;

    private HandlerThread networkThread;
    private HandlerThread offlineThread;
    private Handler networkHandler;
    private Handler offlineHandler;

    protected TelegramApplication application;

    protected SharedPreferences preferences;

    protected BaseSync(TelegramApplication application, String prefsName) {
        this.TAG = getClass().getSimpleName();
        this.application = application;
        this.preferences = application.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        this.networkThread = new HandlerThread("BackgroundNetworkSyncThread", Thread.MIN_PRIORITY);
        this.offlineThread = new HandlerThread("BackgroundOfflineSyncThread", Thread.MIN_PRIORITY);
        this.networkThread.start();
        this.offlineThread.start();
    }

    protected void init() {
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
            requestSyncDelayed(entity.id, Math.max(delta, 1500));
        }
    }

    private Handler getHandler(SyncHolder holder) {
        if (syncHandlers == null) {
            return null;
        }

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

    protected void registerSyncEventAnon(int id, String methodName) {
        registerSync(id, methodName, Integer.MAX_VALUE, false, false, false, false);
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
            entity.isInvalidatedDuringExecution = false;
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
            preferences.edit().putLong("background.sync_" + syncId, 0L).commit();
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
        preferences.edit().putLong("background.sync_" + syncId, System.currentTimeMillis()).commit();
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
        if (handler == null) {
            return;
        }

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
