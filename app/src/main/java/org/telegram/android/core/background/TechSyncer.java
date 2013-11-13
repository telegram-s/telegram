package org.telegram.android.core.background;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;
import org.telegram.api.TLConfig;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestAccountRegisterDevice;
import org.telegram.api.requests.TLRequestAccountUnregisterDevice;
import org.telegram.api.requests.TLRequestHelpGetConfig;

import java.io.IOException;

/**
 * Author: Korshakov Stepan
 * Created: 13.08.13 18:57
 */
public class TechSyncer {

    private static final String TAG = "Tech";

    private static final int PING_DISCONNECT_TIMEOUT = 75 * 1000;// 75 sec
    private static final int PING_TIMEOUT = 60 * 1000;// 60 sec
    private static final long DC_SYNC_TIMEOUT = 24 * 60 * 60 * 1000;// 24 hours
    private static final int DC_SCHEME_NO = 1;

    private Thread syncThread;
    private Handler handler;

    private StelsApplication application;

    private SharedPreferences preferences;

    public TechSyncer(StelsApplication application) {
        this.application = application;

        this.preferences = application.getSharedPreferences("org.telegram.android", Context.MODE_PRIVATE);

        this.syncThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 0) {
                            handleRegisterPush();
                        } else if (msg.what == 1) {
                            handleUnregisterPush();
                        } else if (msg.what == 2) {
                            handleSyncDC();
                        } else if (msg.what == 3) {
                            if (!TechSyncer.this.application.isLoggedIn()) {
                                return;
                            }
                            handlePing();
                        }
                    }
                };
                Looper.loop();
            }
        };
        this.syncThread.setName("TechSyncThread#" + hashCode());
        this.syncThread.start();
        while (handler == null) {
            Thread.yield();
        }
    }

    private void handleRegisterPush() {
        if (!application.isLoggedIn())
            return;
        if (application.getApi() == null)
            return;

        String token;
        synchronized (preferences) {
            token = this.preferences.getString("push_token", null);
            boolean pushSent = this.preferences.getBoolean("push_registered", false);
            if (pushSent)
                return;
        }
        String versionName = "unknown";
        try {
            versionName = application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.t(TAG, e);
        }
        try {
            application.getApi().doRpcCall(new TLRequestAccountRegisterDevice(2, token, Build.MODEL, Build.VERSION.RELEASE, versionName, false, application.getString(R.string.st_lang)));

            synchronized (preferences) {
                String prefToken = this.preferences.getString("push_token", null);
                if (token.equals(prefToken)) {
                    this.preferences.edit().putBoolean("push_registered", true).commit();
                }
            }
        } catch (RpcException e) {
            Logger.t(TAG, e);
        } catch (IOException e) {
            Logger.t(TAG, e);
            handler.removeMessages(0);
            handler.sendEmptyMessageDelayed(0, 15000);
        }
    }

    private void handleUnregisterPush() {
        if (!application.isLoggedIn())
            return;
        if (application.getApi() == null)
            return;

        String token;
        synchronized (preferences) {
            token = this.preferences.getString("push_token", null);
            boolean pushSent = this.preferences.getBoolean("push_registered", false);
            if (!pushSent)
                return;
        }
        try {
            application.getApi().doRpcCall(new TLRequestAccountUnregisterDevice(2, token));

            synchronized (preferences) {
                String prefToken = this.preferences.getString("push_token", null);
                if (token.equals(prefToken)) {
                    this.preferences.edit().putBoolean("push_registered", false).commit();
                }
            }
        } catch (RpcException e) {
            Logger.t(TAG, e);
        } catch (IOException e) {
            Logger.t(TAG, e);
            handler.removeMessages(1);
            handler.sendEmptyMessageDelayed(1, 15000);
        }
    }

    private void handleSyncDC() {
        if (!foregroundCheckDc()) {
            handler.removeMessages(2);
            handler.sendEmptyMessageDelayed(2, 1000);
        }
    }

    private void handlePing() {
        // TODO: Implement ping
        // application.getConnector().getOverlord().postMessage(new PingDelayDisconnect(CryptoUtils.newSecureLong(), PING_DISCONNECT_TIMEOUT), PING_TIMEOUT);
        handler.removeMessages(3);
        handler.sendEmptyMessageDelayed(3, PING_TIMEOUT);
    }

    public void registerPush(String token) {
        synchronized (preferences) {
            String oldToken = this.preferences.getString("push_token", null);
            boolean pushSent = this.preferences.getBoolean("push_registered", false);

            if (!token.equals(oldToken) || !pushSent) {
                this.preferences.edit().putString("push_token", token)
                        .putBoolean("push_registered", false).commit();
                handler.removeMessages(0);
                handler.sendEmptyMessage(0);
            }
        }
    }

    public void unregisterPush(String token) {
        synchronized (preferences) {
            String oldToken = this.preferences.getString("push_token", null);
            boolean pushSent = this.preferences.getBoolean("push_registered", false);

            if (token.equals(oldToken)) {
                this.preferences.edit().putString("push_token", token)
                        .putBoolean("push_registered", true).commit();
                handler.removeMessages(0);
                if (!pushSent) {
                    handler.removeMessages(1);
                    handler.sendEmptyMessage(1);
                }
            }
        }
    }

    public void checkDC() {
        if (application.getApi() == null)
            return;

        long lastSyncTime;
        synchronized (preferences) {
            lastSyncTime = preferences.getLong("dc_sync_time_" + DC_SCHEME_NO, 0);
        }
        if (System.currentTimeMillis() - lastSyncTime > DC_SYNC_TIMEOUT || System.currentTimeMillis() < lastSyncTime) {
            handler.removeMessages(2);
            handler.sendEmptyMessage(2);
        }
    }

    public void checkPush() {
        if (!application.isLoggedIn())
            return;
        if (application.getApi() == null)
            return;
        synchronized (preferences) {
            String oldToken = this.preferences.getString("push_token", null);
            boolean pushSent = this.preferences.getBoolean("push_registered", false);
            if (oldToken != null && !pushSent) {
                handler.removeMessages(0);
                handler.sendEmptyMessage(0);
            }
        }
    }

    public void forcePush() {
        if (!application.isLoggedIn())
            return;
        if (application.getApi() == null)
            return;
        synchronized (preferences) {
            String oldToken = this.preferences.getString("push_token", null);
            if (oldToken != null) {
                this.preferences.edit().putBoolean("push_registered", false).commit();
                handler.removeMessages(0);
                handler.sendEmptyMessage(0);
            }
        }
    }

    public boolean foregroundCheckDc() {
        if (application.getApi() == null)
            return false;

        long lastSyncTime;
        synchronized (preferences) {
            lastSyncTime = preferences.getLong("dc_sync_time_" + DC_SCHEME_NO, 0);
        }
        if (System.currentTimeMillis() - lastSyncTime > DC_SYNC_TIMEOUT || System.currentTimeMillis() < lastSyncTime) {
            return doSyncDC();
        }
        return true;
    }

    public boolean doSyncDC() {
        if (application.getApi() == null)
            return false;

        try {
            TLConfig config = application.getApi().doRpcCall(new TLRequestHelpGetConfig());
            application.getApiStorage().updateSettings(config);
            synchronized (preferences) {
                preferences.edit().putLong("dc_sync_time_" + DC_SCHEME_NO, System.currentTimeMillis()).commit();
            }
            return true;
        } catch (RpcException e) {
            Logger.t(TAG, e);
        } catch (IOException e) {
            Logger.t(TAG, e);
        }
        return false;
    }

    public void doSyncDynamic() {

    }

    public void onLogin() {
        handler.removeMessages(3);
        handler.sendEmptyMessage(3);
        forcePush();
    }

    public void onLogout() {
        preferences.edit().putBoolean("push_registered", false).commit();
        handler.removeMessages(3);
    }
}