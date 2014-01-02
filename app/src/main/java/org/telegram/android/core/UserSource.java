package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.User;

import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:36
 */
public class UserSource {
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

    public void notifyUserChanged(final int uid) {
        final User u = application.getEngine().getUser(uid);
        if (u != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (UserSourceListener listener : listeners) {
                        listener.onUserChanged(uid, u);
                    }
                }
            });
        }
    }
}
