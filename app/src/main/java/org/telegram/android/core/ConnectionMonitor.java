package org.telegram.android.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 03.10.13
 * Time: 0:28
 */
public class ConnectionMonitor {

    private static final String TAG = "ConnectionMonitor";
    private static final long DEFAULT_TIMEOUT = 5 * 60 * 1000;
    private TelegramApplication application;

    private ConnectivityManager connectivityManager;

    private final Object locker = new Object();

    public ConnectionMonitor(TelegramApplication application) {
        this.application = application;
        this.connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);

        application.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkConnection();
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public boolean isConnected() {
        NetworkInfo i = connectivityManager.getActiveNetworkInfo();
        if (i == null)
            return false;
        if (!i.isConnected())
            return false;
        if (!i.isAvailable())
            return false;
        return true;
    }

    private void checkConnection() {
        synchronized (locker) {
            if (isConnected()) {
                locker.notifyAll();
            }
        }
    }

    public void waitForConnection() {
        waitForConnection(DEFAULT_TIMEOUT);
    }

    public void waitForConnection(long timeout) {
        if (isConnected()) {
            return;
        }
        synchronized (locker) {
            if (isConnected()) {
                return;
            }
            Logger.d(TAG, "Waiting for connection...");
            try {
                locker.wait(timeout);
            } catch (InterruptedException e) {
                Logger.t(TAG, e);
            }
            Logger.d(TAG, "Completed waiting");
        }
    }
}
