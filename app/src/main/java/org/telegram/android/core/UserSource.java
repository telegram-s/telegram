package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:36
 */
public class UserSource {
    private static final String TAG = "UserSource";

    private final CopyOnWriteArrayList<UserSourceListener> listeners = new CopyOnWriteArrayList<UserSourceListener>();

    private StelsApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());

    public UserSource(StelsApplication application) {
        this.application = application;
    }

    public void registerListener(UserSourceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(UserSourceListener listener) {
        listeners.remove(listener);
    }

    public void notifyUsersChanged(final User[] u) {
        if (u != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Logger.d(TAG, "notify");
                    for (UserSourceListener listener : listeners) {
                        listener.onUsersChanged(u);
                    }
                }
            });
        }
    }
}
