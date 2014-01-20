package org.telegram.android.core.background;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.ChatMessage;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:27
 */
public class SelfDestructProcessor {

    private static final int MSG_INITIAL_DELETIONS = 0;
    private static final int MSG_DESTROY = 1;

    private TelegramApplication application;
    private HandlerThread thread;
    private Handler handler;

    public SelfDestructProcessor(TelegramApplication _application) {
        this.application = _application;
    }

    public void runProcessor() {
        this.thread = new HandlerThread("SelfDestructThread");
        this.thread.start();
        while (this.thread.getLooper() == null) {
            Thread.yield();
        }
        handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_INITIAL_DELETIONS) {
                    checkInitialDeletions();
                } else if (msg.what == MSG_DESTROY) {
                    ChatMessage message = application.getEngine().getMessageByDbId(msg.arg1);
                    if (message != null) {
                        application.getEngine().selfDestructMessage(message.getDatabaseId());
                        application.notifyUIUpdate();
                    }
                }
            }
        };
    }


    public void performSelfDestruct(int databaseId, int time) {
        handler.removeMessages(databaseId);
        Message message = handler.obtainMessage(MSG_DESTROY, databaseId, 0);
        handler.sendMessageAtTime(message, (SystemClock.uptimeMillis() - System.currentTimeMillis()) + time * 1000L);
    }

    public void requestInitialDeletions() {
        handler.sendEmptyMessage(MSG_INITIAL_DELETIONS);
    }

    private void checkInitialDeletions() {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        ChatMessage[] messages = application.getEngine().getMessagesEngine().findDiedMessages(currentTime);
        for (ChatMessage msg : messages) {
            application.getEngine().selfDestructMessage(msg.getDatabaseId());
        }
        ChatMessage[] pending = application.getEngine().getMessagesEngine().findPendingSelfDestructMessages(currentTime);
        for (ChatMessage msg : pending) {
            performSelfDestruct(msg.getDatabaseId(), msg.getMessageDieTime());
        }
    }

}
