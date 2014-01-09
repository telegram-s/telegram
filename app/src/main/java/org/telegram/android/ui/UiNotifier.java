package org.telegram.android.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import org.telegram.android.log.Logger;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Korshakov Stepan
 * Created: 02.09.13 18:18
 */
public class UiNotifier {
    private static final String TAG = "UiNotifier";
    private static long UI_DELAY = 400;
    private static long NOTIFY_DELAY = 300;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Logger.d(TAG, "notify");
            if (msg.what == 0) {
                executePendingNotifications();
            } else if (msg.what == 1) {
                if (SystemClock.uptimeMillis() - uiNotifyTime > UI_DELAY) {
                    executePendingNotifications();
                } else {
                    handler.removeMessages(0);
                    handler.sendEmptyMessageDelayed(0, SystemClock.uptimeMillis() - uiNotifyTime);
                }
            }
        }
    };
    private long uiNotifyTime = SystemClock.uptimeMillis();
    private ConcurrentHashMap<Object, Runnable> notifications = new ConcurrentHashMap<Object, Runnable>();

    public void notify(final Object key, final Runnable runnable) {
        notifications.put(key, runnable);
        handler.removeMessages(1);
        handler.sendEmptyMessageDelayed(1, NOTIFY_DELAY);
    }

    public void executePendingNotifications() {
        for (Runnable runnable : notifications.values()) {
            runnable.run();
        }
        notifications.clear();
    }
}
