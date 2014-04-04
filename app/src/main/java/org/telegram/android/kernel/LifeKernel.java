package org.telegram.android.kernel;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.BackgroundService;
import org.telegram.android.log.Logger;
import org.telegram.api.engine.TelegramApi;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class LifeKernel {

    private static final String TAG = "LifeKernel";

    private static long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    private static final long KEEP_TIME = 3 * 60 * 1000;

    private ApplicationKernel kernel;

    private boolean forceUiLife;
    private boolean forceWaitForUpdate;

    private long lastVisibleTime;
    private long lastUpdateRequired;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            forceLowPower();
        }
    };

    public LifeKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        this.lastVisibleTime = getCurrentTime();
        this.lastUpdateRequired = getCurrentTime();
    }

    public void onAppVisible() {
        forceUiLife = true;
        lastVisibleTime = getCurrentTime();
        check();
    }

    public void onAppHidden() {
        forceUiLife = false;
        check();
    }

    public void onUpdateRequired() {
        forceWaitForUpdate = true;
        lastUpdateRequired = getCurrentTime();
        check();
    }

    public void onUpdateReceived() {
        forceWaitForUpdate = false;
        check();
    }

    public boolean isForcedKeepAlive() {
        return forceUiLife;
    }

    public long dieTimeout() {
        if (isForcedKeepAlive()) {
            return Long.MAX_VALUE;
        } else {
            if (forceWaitForUpdate) {
                return Math.max(KEEP_TIME - (getCurrentTime() - lastVisibleTime), Math.max(0, KEEP_TIME - (getCurrentTime() - lastUpdateRequired)));
            } else {
                return Math.max(0, KEEP_TIME - (getCurrentTime() - lastVisibleTime));
            }
        }
    }

    public void check() {
        forceFullPower();
        handler.removeMessages(0);
        long delay = dieTimeout();
        if (delay != Long.MAX_VALUE) {
            handler.sendEmptyMessageDelayed(0, delay);
        }
    }

    public void forceFullPower() {
        Logger.d(TAG, "Force Full Power");
        kernel.getApiKernel().getApi().switchMode(TelegramApi.MODE_FULL_POWER);
    }

    public void forceLowPower() {
        Logger.d(TAG, "Force Low Power");
        kernel.getApiKernel().getApi().switchMode(TelegramApi.MODE_LOW_POWER);
    }

    public void runKernel() {
        // Keep app working forever
        kernel.getApplication().startService(new Intent(kernel.getApplication(), BackgroundService.class));
        check();
    }
}
