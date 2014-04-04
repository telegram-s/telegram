package org.telegram.android;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import org.telegram.android.kernel.ApplicationKernel;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class BackgroundService extends Service {
    private final String TAG;

//    private static final long CHECK_TIMEOUT = 15 * 1000;

//    private Handler handler = new Handler(Looper.getMainLooper());
//    private ApplicationKernel kernel;
//    private Runnable checkRunnable;

    public BackgroundService() {
        TAG = "BackgroundService#" + hashCode();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate");
//        kernel = ((TelegramApplication) getApplication()).getKernel();
//        checkRunnable = new Runnable() {
//            @Override
//            public void run() {
//                checkTimeout();
//            }
//        };
//        checkTimeout();
    }

//    private void checkTimeout() {
//        org.telegram.android.log.Logger.d(TAG, "notify");
//        long timeout = Math.min(CHECK_TIMEOUT, kernel.getLifeKernel().dieTimeout());
//        Logger.d(TAG, "checkTimeout: " + timeout);
//        if (timeout == 0) {
//            Logger.d(TAG, "stopping service");
//            stopSelf();
//        } else {
//            handler.removeCallbacks(checkRunnable);
//            handler.postDelayed(checkRunnable, timeout);
//        }
//    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");
    }
}
