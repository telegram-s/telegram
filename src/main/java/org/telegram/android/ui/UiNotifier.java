package org.telegram.android.ui;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: Korshakov Stepan
 * Created: 02.09.13 18:18
 */
public class UiNotifier {
    private static long UI_DELAY = 400;
    private static long NOTIFY_DELAY = 300;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            executePendingNotifications();
        }
    };
    private long uiNotifyTime = SystemClock.uptimeMillis();
    private ConcurrentHashMap<Object, Runnable> notifications = new ConcurrentHashMap<Object, Runnable>();

    public void notify(final Object key, final Runnable runnable) {
        notifications.put(key, runnable);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (SystemClock.uptimeMillis() - uiNotifyTime > UI_DELAY) {
                    executePendingNotifications();
                } else {
                    handler.removeMessages(0);
                    handler.sendEmptyMessageDelayed(0, SystemClock.uptimeMillis() - uiNotifyTime);
                }
            }
        }, NOTIFY_DELAY);
    }

    public void executePendingNotifications() {
        for (Runnable runnable : notifications.values()) {
            runnable.run();
        }
        notifications.clear();
    }
}
