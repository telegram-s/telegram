package org.telegram.notifications;

/**
 * Created by ex3ndr on 20.03.14.
 */
public interface NotificationDispatcher {
    void dispatchNotification(Runnable runnable);
}
