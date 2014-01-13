package org.telegram.android.kernel;

import android.content.Intent;
import org.telegram.android.BackgroundService;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class LifeKernel {

    private static long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    private static final long KEEP_TIME = 5 * 60 * 1000;

    private ApplicationKernel kernel;

    private boolean forceUiLife;
    private boolean forceWaitForUpdate;

    private long lastVisibleTime;
    private long lastUpdateRequired;

    public LifeKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        this.lastVisibleTime = getCurrentTime();
        this.lastUpdateRequired = getCurrentTime();
    }

    public void onAppVisible() {
        forceUiLife = true;
        lastVisibleTime = getCurrentTime();
        startService();
    }

    public void onAppHidden() {
        forceUiLife = false;
        startService();
    }

    public void onUpdateRequired() {
        forceWaitForUpdate = true;
        lastUpdateRequired = getCurrentTime();
        startService();
    }

    public void onUpdateReceived() {
        forceWaitForUpdate = false;
        startService();
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

    public void runKernel() {
        startService();
    }

    private void startService() {
        kernel.getApplication().startService(new Intent(kernel.getApplication(), BackgroundService.class));
    }
}
