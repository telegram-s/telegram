package org.telegram.android.tasks;

import android.os.Handler;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 16:59
 */
public class CallBarrier {
    private Reference<CallbackHandler> callbackHandler;

    private CopyOnWriteArrayList<Runnable> callbacks =
            new CopyOnWriteArrayList<Runnable>();
    private boolean isPaused;
    private boolean isResuming;
    private final Object pausedLocker = new Object();
    private Handler uiHandler;

    public CallBarrier(CallbackHandler handler, Handler uiHandler) {
        this.uiHandler = uiHandler;
        this.callbackHandler = new WeakReference<CallbackHandler>(handler);
        this.isPaused = false;
    }

    public void sendCallback(Runnable runnable) {
        CallbackHandler handler = callbackHandler.get();
        if (handler != null) {
            synchronized (pausedLocker) {
                if (!isPaused) {
                    handler.receiveCallback(runnable);
                } else {
                    callbacks.add(runnable);
                }
            }
        } else {
            // Log.w(Tags.TAG_BARRIER, "CallbackHandler died");
        }
        // Else reference died
    }

    public void sendCallbackWeak(Runnable runnable) {
        CallbackHandler handler = callbackHandler.get();
        if (handler != null) {
            synchronized (pausedLocker) {
                if (!isPaused) {
                    handler.receiveCallback(runnable);
                }
            }
        } else {
            // Log.w(Tags.TAG_BARRIER, "CallbackHandler died");
        }
        // Else reference died
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void pause() {
        synchronized (pausedLocker) {
            isPaused = true;
            isResuming = false;
        }
    }

    public void resume() {
        synchronized (pausedLocker) {
            if (isResuming) {
                return;
            }
            isPaused = false;
        }

        for (Runnable runnable : callbacks) {
            sendCallback(runnable);
        }
        callbacks.clear();
    }

    public void resume(int delay) {
        synchronized (pausedLocker) {
            if (isResuming) {
                return;
            }
            isResuming = true;
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    completeResume();
                }
            }, delay);
        }
    }

    private void completeResume() {
        synchronized (pausedLocker) {
            if (!isResuming) {
                return;
            }
            isPaused = false;
            isResuming = false;
        }
        for (Runnable runnable : callbacks) {
            sendCallback(runnable);
        }
        callbacks.clear();
    }
}