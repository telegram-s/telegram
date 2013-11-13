package org.telegram.android.core.background;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.ChatMessage;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:27
 */
public class SelfDestructProcessor {
    private StelsApplication application;
    private HandlerThread thread = new HandlerThread("SelfDestructThread");
    private Handler handler;

    public SelfDestructProcessor(StelsApplication _application) {
        this.application = _application;
        this.thread.start();
        while (this.thread.getLooper() == null) {
            Thread.yield();
        }
        handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                ChatMessage message = application.getEngine().getMessageByDbId(msg.what);
                if (message != null) {
                    application.getEngine().seldfDestructMessage(message.getDatabaseId());
                    application.notifyUIUpdate();
                }
            }
        };
    }

    public void performSelfDestruct(int databaseId, int time) {
        handler.removeMessages(databaseId);
        handler.sendEmptyMessageAtTime(databaseId, (SystemClock.uptimeMillis() - System.currentTimeMillis()) + time * 1000L);
    }

    public void checkInitialDeletions() {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        ChatMessage[] messages = application.getEngine().findDiedMessages(currentTime);
        for (ChatMessage msg : messages) {
            application.getEngine().seldfDestructMessage(msg.getDatabaseId());
        }
        ChatMessage[] pending = application.getEngine().findPendingSelfDestructMessages(currentTime);
        for (ChatMessage msg : pending) {
            performSelfDestruct(msg.getDatabaseId(), msg.getMessageDieTime());
        }
    }

}
