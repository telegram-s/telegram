package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.EncryptedChat;
import org.telegram.android.log.Logger;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:36
 */
public class EncryptedChatSource {
    private static final String TAG = "EncryptedChatSource";
    private final CopyOnWriteArrayList<EncryptedChatSourceListener> listeners = new CopyOnWriteArrayList<EncryptedChatSourceListener>();

    private TelegramApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());

    public EncryptedChatSource(TelegramApplication application) {
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
                    Logger.d(TAG, "notify");
                    for (EncryptedChatSourceListener listener : listeners) {
                        listener.onEncryptedChatChanged(chatId, u);
                    }
                }
            });
        }
    }
}