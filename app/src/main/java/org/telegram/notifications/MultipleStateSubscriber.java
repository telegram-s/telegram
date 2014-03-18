package org.telegram.notifications;

/**
 * Created by ex3ndr on 18.03.14.
 */
public interface MultipleStateSubscriber {
    void onStateChanged(int kind, long id, int state, Object... args);
}
