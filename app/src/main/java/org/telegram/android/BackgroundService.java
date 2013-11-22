package org.telegram.android;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import org.telegram.android.kernel.ApplicationKernel;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class BackgroundService extends Service {

    private static final long CHECK_TIMEOUT = 15 * 1000;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ApplicationKernel kernel;
    private Runnable checkRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        kernel = ((StelsApplication) getApplication()).getKernel();
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkTimeout();
            }
        };
    }

    private void checkTimeout() {
        long timeout = Math.min(CHECK_TIMEOUT, kernel.getLifeKernel().dieTimeout());
        if (timeout == 0) {
            stopSelf();
        } else {
            handler.removeCallbacks(checkRunnable);
            handler.postDelayed(checkRunnable, timeout);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
