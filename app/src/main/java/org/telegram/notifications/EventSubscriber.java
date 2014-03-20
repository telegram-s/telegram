package org.telegram.notifications;

/**
 * Created by ex3ndr on 18.03.14.
 */
public interface EventSubscriber {
    void onEvent(int kind, Object... args);
}
