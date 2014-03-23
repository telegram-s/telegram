package org.telegram.threading;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class ActorThread extends HandlerThread {

    private volatile Handler handler;
    private final ArrayList<DeliverMessage> pendingMessages = new ArrayList<DeliverMessage>();
    private final ArrayList<DeliverMessage> freeDeliver = new ArrayList<DeliverMessage>();

    private static final int MESSAGE = 0;
    private static final int POISON = 1;

    public ActorThread(String name) {
        super(name);
    }

    public ActorThread(String name, int priority) {
        super(name, priority);
    }

    @Override
    protected void onLooperPrepared() {
        synchronized (pendingMessages) {
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE) {
                        DeliverMessage arg = (DeliverMessage) msg.obj;
                        try {
                            arg.actor.receiveMessage(arg.message, arg.args, arg.sender);
                        } catch (Exception e) {
                            arg.actor.onException(e);
                        }
                        releaseMessage(arg);
                    } else if (msg.what == POISON) {
                        getLooper().quit();
                    }
                }
            };
            for (DeliverMessage message : pendingMessages) {
                Message message1 = handler.obtainMessage(MESSAGE, message);
                handler.sendMessage(message1);
            }
            pendingMessages.clear();
        }
    }

    private void releaseMessage(DeliverMessage message) {
        message.actor = null;
        message.message = null;
        synchronized (freeDeliver) {
            freeDeliver.add(message);
        }
    }

    private DeliverMessage obtainMessage() {
        synchronized (freeDeliver) {
            if (freeDeliver.size() == 0) {
                return new DeliverMessage();
            } else {
                return freeDeliver.remove(0);
            }
        }
    }

    public void deliverMessage(Actor reference, String message, Object[] args, ActorReference sender) {
        deliverMessageDelayed(reference, message, args, sender, 0);
    }

    public void deliverMessageDelayed(Actor reference, String message, Object[] args, ActorReference sender, long delay) {
        DeliverMessage deliverMessage = obtainMessage();
        deliverMessage.actor = reference;
        deliverMessage.message = message;
        deliverMessage.args = args;
        deliverMessage.sender = sender;
        synchronized (pendingMessages) {
            if (handler == null) {
                pendingMessages.add(deliverMessage);
            } else {
                Message message1 = handler.obtainMessage(MESSAGE, deliverMessage);
                if (delay > 0) {
                    handler.sendMessageDelayed(message1, delay);
                } else {
                    handler.sendMessage(message1);
                }
            }
        }
    }

    public void close() {
        if (handler != null) {
            handler.sendEmptyMessage(POISON);
        }
    }

    private class DeliverMessage {
        public Actor actor;
        public String message;
        public Object[] args;
        public ActorReference sender;

        private DeliverMessage() {

        }
    }
}
