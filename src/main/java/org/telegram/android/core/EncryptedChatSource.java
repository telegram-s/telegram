package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.EncryptedChat;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:36
 */
public class EncryptedChatSource {
    private final CopyOnWriteArrayList<EncryptedChatSourceListener> listeners = new CopyOnWriteArrayList<EncryptedChatSourceListener>();

    private StelsApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());

    public EncryptedChatSource(StelsApplication application) {
        this.application = application;
    }

    public void registerListener(EncryptedChatSourceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(EncryptedChatSourceListener listener) {
        listeners.remove(listener);
    }

    public void notifyChatChanged(final int chatId) {
        final EncryptedChat u = application.getEngine().getEncryptedChat(chatId);
        if (u != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    for (EncryptedChatSourceListener listener : listeners) {
                        listener.onEncryptedChatChanged(chatId, u);
                    }
                }
            });
        }
    }
}