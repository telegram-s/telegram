package org.telegram.android.core;

import android.os.Handler;
import android.os.Looper;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.FullChatInfo;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.local.TLLocalChatParticipant;
import org.telegram.android.log.Logger;
import org.telegram.api.messages.TLChatFull;
import org.telegram.api.requests.TLRequestMessagesGetFullChat;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:36
 */
public class ChatSource {
    private static final String TAG = "ChatSource";

    private HashMap<Integer, FullChatInfo> chatInfos = new HashMap<Integer, FullChatInfo>();

    private HashSet<Integer> requestedChats = new HashSet<Integer>();

    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("ChatSourceService#" + res.hashCode());
            return res;
        }
    });

    private final CopyOnWriteArrayList<ChatSourceListener> listeners = new CopyOnWriteArrayList<ChatSourceListener>();

    private StelsApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());

    public ChatSource(StelsApplication application) {
        this.application = application;
    }

    public void registerListener(ChatSourceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(ChatSourceListener listener) {
        listeners.remove(listener);
    }

    public void notifyChatChanged(final int chatId) {
        final DialogDescription u = application.getEngine().getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (u != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Logger.d(TAG, "notify");
                    for (ChatSourceListener listener : listeners) {
                        listener.onChatChanged(chatId, u);
                    }
                }
            });
        }
    }

    public void loadFullChat(final int chatId) {
        synchronized (requestedChats) {
            if (requestedChats.contains(chatId))
                return;
            requestedChats.add(chatId);
        }

        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FullChatInfo info = application.getEngine().getFullChatInfo(chatId);
                    if (info != null) {
                        updateChatInfo(info);
                    } else {
                        TLChatFull full = application.getApi().doRpcCall(new TLRequestMessagesGetFullChat(chatId));
                        application.getEngine().onUsers(full.getUsers());
                        application.getEngine().getGroupsEngine().onGroupsUpdated(full.getChats());
                        application.getEngine().getFullGroupEngine().onFullChat(full.getFullChat());
                    }
                    notifyChatChanged(chatId);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    synchronized (requestedChats) {
                        requestedChats.remove(chatId);
                    }
                }
            }
        });
    }

    public FullChatInfo getChatInfo(int chatId) {
        FullChatInfo res = chatInfos.get(chatId);
        if (res == null) {
            loadFullChat(chatId);
        }
        return res;
    }

    public void updateChatInfo(FullChatInfo info) {
        for (TLLocalChatParticipant uid : info.getChatInfo().getUsers()) {
            application.getEngine().getUser(uid.getUid());
        }
        chatInfos.put(info.getChatId(), info);
    }

    public void clear() {
        chatInfos.clear();
    }
}