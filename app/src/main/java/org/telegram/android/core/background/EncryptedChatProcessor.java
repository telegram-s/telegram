package org.telegram.android.core.background;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.EncryptedChat;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 22.10.13
 * Time: 3:12
 */
public class EncryptedChatProcessor {
    private static final String TAG = "EncryptedChatProcessor";
    private HandlerThread actionsThread;
    private Handler handler;

    private StelsApplication application;

    public EncryptedChatProcessor(StelsApplication _application) {
        this.application = _application;
        actionsThread = new HandlerThread("EncryptedChatProcessorThread");
        actionsThread.start();
        while (actionsThread.getLooper() == null) {
            Thread.yield();
        }
        handler = new Handler(actionsThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (!application.isLoggedIn()) {
                    return;
                }

                try {
                    EncryptedChat[] chats = application.getEngine().getPendingEncryptedChats();
                    for (EncryptedChat chat : chats) {
                        application.getEncryptionController().confirmEncryption(chat.getId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.removeMessages(0);
                    handler.sendEmptyMessageDelayed(0, 1000);
                }
            }
        };
    }

    public void checkChats() {
        if (application.getUiKernel().isAppActive()) {
            handler.removeMessages(0);
            handler.sendEmptyMessage(0);
        }
    }

    public void onUserGoesOnline() {
        if (application.getUiKernel().isAppActive()) {
            handler.removeMessages(0);
            handler.sendEmptyMessage(0);
        }
    }

    public void onUserGoesOffline() {
        handler.removeMessages(0);
    }
}
