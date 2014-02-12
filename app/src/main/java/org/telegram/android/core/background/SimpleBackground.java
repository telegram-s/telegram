package org.telegram.android.core.background;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by ex3ndr on 12.02.14.
 */
public class SimpleBackground {
    private HandlerThread thread;
    private Handler handler;

    public SimpleBackground() {

    }

    public void run() {
        thread = new HandlerThread("SimpleBackground");
        thread.start();
        while (thread.getLooper() == null) {
            Thread.yield();
        }
        handler = new Handler(thread.getLooper());
    }

    public void postAction(Runnable runnable) {
        handler.post(runnable);
    }
}
