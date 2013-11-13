package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.mtproto.time.TimeOverlord;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 2:20
 */
public class TypingStates {

    public interface TypingListener {
        public void onChatTypingChanged(int chatId, int[] uids);

        public void onUserTypingChanged(int uid, boolean types);

        public void onEncryptedTypingChanged(int chatId, boolean types);
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                int[] types = getChatTypes(msg.arg2);
                for (WeakReference<TypingListener> listener : listeners) {
                    TypingListener l = listener.get();
                    if (l != null) {
                        l.onChatTypingChanged(msg.arg2, types);
                    }
                }
                long nextChange = getNextTypesChanges(msg.arg2);
                if (nextChange != Long.MAX_VALUE) {
                    handler.sendMessageDelayed(handler.obtainMessage(msg.what, msg.arg1, msg.arg2), nextChange);
                }
            } else if (msg.arg1 == 1) {
                boolean types = isUserTyping(msg.arg2);
                for (WeakReference<TypingListener> listener : listeners) {
                    TypingListener l = listener.get();
                    if (l != null) {
                        l.onUserTypingChanged(msg.arg2, types);
                    }
                }
                long nextChange = getNextUserTypesChanges(msg.arg2);
                if (nextChange != Long.MAX_VALUE) {
                    handler.sendMessageDelayed(handler.obtainMessage(msg.what, msg.arg1, msg.arg2), nextChange);
                }
            } else if (msg.arg1 == 2) {
                boolean types = isEncryptedTyping(msg.arg2);
                for (WeakReference<TypingListener> listener : listeners) {
                    TypingListener l = listener.get();
                    if (l != null) {
                        l.onEncryptedTypingChanged(msg.arg2, types);
                    }
                }
                long nextChange = getNextEncryptedTypesChanges(msg.arg2);
                if (nextChange != Long.MAX_VALUE) {
                    handler.sendMessageDelayed(handler.obtainMessage(msg.what, msg.arg1, msg.arg2), nextChange);
                }
            }
        }
    };

    private static final int TYPING_TIMEOUT = 5000;//Sec

    private HashMap<Integer, Long> encryptedTypes = new HashMap<Integer, Long>();

    private HashMap<Integer, Long> userTypes = new HashMap<Integer, Long>();

    private HashMap<Integer, HashMap<Integer, Long>> chatTypes = new HashMap<Integer, HashMap<Integer, Long>>();

    private CopyOnWriteArrayList<WeakReference<TypingListener>> listeners = new CopyOnWriteArrayList<WeakReference<TypingListener>>();

    private StelsApplication application;

    public TypingStates(StelsApplication application) {
        this.application = application;
    }

    public void registerListener(TypingListener listener) {
        for (WeakReference<TypingListener> l : listeners) {
            if (l.get() == listener)
                return;
        }
        listeners.add(new WeakReference<TypingListener>(listener));
    }

    public void unregisterListener(TypingListener listener) {
        for (WeakReference<TypingListener> l : listeners) {
            if (l.get() == listener) {
                listeners.remove(l);
                return;
            }
        }
    }

    private void notifyChatChange(int chatId) {
        handler.removeMessages(chatId * 3);
        handler.sendMessage(handler.obtainMessage(chatId * 3, 0, chatId));
    }

    private void notifyUserChange(int chatId) {
        handler.removeMessages(chatId * 3 + 1);
        handler.sendMessage(handler.obtainMessage(chatId * 3 + 1, 1, chatId));
    }

    private void notifyEncryptedChange(int chatId) {
        handler.removeMessages(chatId * 3 + 2);
        handler.sendMessage(handler.obtainMessage(chatId * 3 + 2, 2, chatId));
    }

    public synchronized boolean isUserTyping(int id) {
        Long res = userTypes.get(id);
        if (res != null) {
            if (TimeOverlord.getInstance().getServerTime() - res < TYPING_TIMEOUT) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean isEncryptedTyping(int id) {
        Long res = encryptedTypes.get(id);
        if (res != null) {
            if (TimeOverlord.getInstance().getServerTime() - res < TYPING_TIMEOUT) {
                return true;
            }
        }

        return false;
    }

    public synchronized long getNextEncryptedTypesChanges(int chatId) {
        long res = Long.MAX_VALUE;

        Long resTime = encryptedTypes.get(chatId);
        if (resTime != null) {
            if (TimeOverlord.getInstance().getServerTime() - resTime < TYPING_TIMEOUT) {
                res = Math.min((TYPING_TIMEOUT - (TimeOverlord.getInstance().getServerTime() - resTime)), res);
            }
        }

        return res;
    }

    public synchronized long getNextUserTypesChanges(int uid) {
        long currentTime = TimeOverlord.getInstance().getServerTime();
        long res = Long.MAX_VALUE;

        Long resTime = userTypes.get(uid);
        if (resTime != null) {
            if (currentTime - resTime < TYPING_TIMEOUT) {
                res = Math.min((TYPING_TIMEOUT - (currentTime - resTime)), res);
            }
        }

        return res;
    }

    public synchronized long getNextTypesChanges(int chatId) {
        long currentTime = TimeOverlord.getInstance().getServerTime();
        long res = Long.MAX_VALUE;
        HashMap<Integer, Long> chatTyping = chatTypes.get(chatId);
        if (chatTyping != null) {
            ArrayList<Integer> uids = new ArrayList<Integer>();
            for (Integer key : chatTyping.keySet()) {
                if (currentTime - chatTyping.get(key) < TYPING_TIMEOUT) {
                    res = Math.min((TYPING_TIMEOUT - (currentTime - chatTyping.get(key))), res);
                }
            }
        }

        return res;
    }

    public synchronized int[] getChatTypes(int chatId) {
        long currentTime = TimeOverlord.getInstance().getServerTime();
        HashMap<Integer, Long> chatTyping = chatTypes.get(chatId);
        if (chatTyping != null) {
            ArrayList<Integer> uids = new ArrayList<Integer>();
            for (Integer key : chatTyping.keySet()) {
                if (currentTime - chatTyping.get(key) < TYPING_TIMEOUT) {
                    uids.add(key);
                }
            }

            int[] res = new int[uids.size()];
            for (int i = 0; i < uids.size(); i++) {
                res[i] = uids.get(i);
            }
            return res;
        }

        return new int[0];
    }

    public synchronized void resetUserTyping(int uid) {
        userTypes.remove(uid);
        notifyUserChange(uid);
    }

    public synchronized void resetEncryptedTyping(int chatId) {
        encryptedTypes.remove(chatId);
        notifyEncryptedChange(chatId);
    }

    public synchronized void resetUserTyping(int uid, int chatId) {
        HashMap<Integer, Long> chatTyping = chatTypes.get(chatId);
        if (chatTyping != null) {
            chatTyping.remove(uid);
        }
        notifyChatChange(chatId);
    }

    public synchronized void onUserTyping(int uid, int time) {
        if (application.getEngine().getUser(uid) == null)
            return;

        Long res = userTypes.get(uid);
        if (res != null) {
            if (res < time * 1000L) {
                userTypes.put(uid, time * 1000L);
            }
        } else {
            userTypes.put(uid, time * 1000L);
        }
        notifyUserChange(uid);
    }

    public synchronized void onEncryptedTyping(int chatId, int time) {
        if (application.getEngine().getEncryptedChat(chatId) == null)
            return;

        Long res = encryptedTypes.get(chatId);
        if (res != null) {
            if (res < time * 1000L) {
                encryptedTypes.put(chatId, time * 1000L);
            }
        } else {
            encryptedTypes.put(chatId, time * 1000L);
        }
        notifyEncryptedChange(chatId);
    }

    public synchronized void onUserChatTyping(int uid, int chatId, int time) {
        if (application.getEngine().getUser(uid) == null)
            return;

        HashMap<Integer, Long> chatTyping = chatTypes.get(chatId);
        if (chatTyping != null) {
            Long res = chatTyping.get(uid);
            if (res != null) {
                if (res < time * 1000L) {
                    chatTyping.put(uid, time * 1000L);
                }
            } else {
                chatTyping.put(uid, time * 1000L);
            }
        } else {
            HashMap<Integer, Long> chat = new HashMap<Integer, Long>();
            chat.put(uid, time * 1000L);
            chatTypes.put(chatId, chat);
        }
        notifyChatChange(chatId);
    }
}
