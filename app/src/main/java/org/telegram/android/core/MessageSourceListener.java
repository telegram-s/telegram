package org.telegram.android.core;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 0:43
 */
public interface MessageSourceListener {
    public void onMessagesChanged();

    public void onMessagesStateChanged();
}
