package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.Configuration;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.cursors.ViewSource;
import org.telegram.android.cursors.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsInputPeer;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerContact;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLMessages;
import org.telegram.api.messages.TLMessagesSlice;
import org.telegram.api.requests.TLRequestMessagesGetHistory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 0:42
 */
public class MessageSource {

    public static void clearData(StelsApplication context) {
        try {
            File shared = new File("/data/data/" + context.getPackageName() + "/shared_prefs/");
            String[] sharedPrefs = shared.list();
            for (String s : sharedPrefs) {
                if (s.startsWith("org.telegram.android.Chat_")) {
                    String name = s.substring(0, s.length() - 4);
                    SharedPreferences pref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    pref.edit().remove("state").commit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int PAGE_SIZE = 25;

    public static final int PAGE_OVERLAP = 3;

    public static final int PAGE_REQUEST_PADDING = 20;

    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageSourceService#" + res.hashCode());
            res.setPriority(Configuration.SOURCES_THREAD_PRIORITY);
            return res;
        }
    });

    private final Object stateSync = new Object();
    private MessagesSourceState state;

    private StelsApplication application;

    private int peerType, peerId;

    private ViewSource<ChatMessage> messagesSource;

    private SharedPreferences preferences;

    private boolean destroyed;

    private final String TAG;

    public MessageSource(int _peerType, int _peerId, StelsApplication _application) {
        TAG = "MessageSource#" + _peerType + "@" + _peerId;
        this.application = _application;
        this.destroyed = false;
        this.preferences = this.application.getSharedPreferences("org.telegram.android.Chat_" + _peerType + "_" + _peerId, Context.MODE_PRIVATE);
        this.state = MessagesSourceState.valueOf(preferences.getString("state", MessagesSourceState.UNSYNCED.name()));
        if (this.state == MessagesSourceState.FULL_SYNC) {
            this.state = MessagesSourceState.UNSYNCED;
        } else if (this.state == MessagesSourceState.LOAD_MORE || this.state == MessagesSourceState.LOAD_MORE_ERROR) {
            this.state = MessagesSourceState.SYNCED;
        }
        this.peerType = _peerType;
        this.peerId = _peerId;
        this.messagesSource = new ViewSource<ChatMessage>(_application.getResponsibility()) {

            @Override
            protected ChatMessage[] loadItems(int offset) {
                if (offset < PAGE_OVERLAP) {
                    offset = 0;
                } else {
                    offset -= PAGE_OVERLAP;
                }
                PreparedQuery<ChatMessage> query;
                try {
                    QueryBuilder<ChatMessage, Long> queryBuilder = application.getEngine().getMessagesDao().queryBuilder();
                    queryBuilder.orderByRaw("-(date * 1000000 + abs(mid))");
                    queryBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType).and().eq("deletedLocal", false);
                    queryBuilder.offset(offset);
                    queryBuilder.limit(PAGE_SIZE);
                    // queryBuilder.selectColumns("_id", "mid", "isOut", "state", "date", "contentType", "senderId", "message");
                    query = queryBuilder.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ChatMessage[0];
                }
                application.getResponsibility().waitForResume();
                ChatMessage[] res = application.getEngine().getMessagesDao().query(query).toArray(new ChatMessage[0]);
                for (int i = 0; i < res.length; i++) {
                    res[i].getExtras();
                    application.getEngine().getUser(res[i].getSenderId());
                    if (res[i].isForwarded()) {
                        application.getEngine().getUser(res[i].getForwardSenderId());
                    }
                }
                return res;
            }

            @Override
            protected long getSortingKey(ChatMessage obj) {
                if (obj.getMid() <= 0) {
                    return obj.getDate() * 1000000L + 999999L;
                } else {
                    return obj.getDate() * 1000000L + Math.abs(obj.getMid());
                }
            }

            @Override
            protected long getItemKey(ChatMessage obj) {
                return obj.getDatabaseId();
            }

            @Override
            protected ViewSourceState getInternalState() {
                switch (state) {
                    default:
                    case UNSYNCED:
                        return ViewSourceState.UNSYNCED;
                    case COMPLETED:
                        return ViewSourceState.COMPLETED;
                    case LOAD_MORE_ERROR:
                        return ViewSourceState.LOAD_MORE_ERROR;
                    case LOAD_MORE:
                        return ViewSourceState.IN_PROGRESS;
                    case FULL_SYNC:
                        return ViewSourceState.IN_PROGRESS;
                    case SYNCED:
                        return ViewSourceState.SYNCED;
                }
            }

            @Override
            protected void onItemRequested(int index) {
                if (index > getItemsCount() - PAGE_REQUEST_PADDING) {
                    requestLoadMore(getItemsCount());
                }
            }
        };
    }

    public ViewSource<ChatMessage> getMessagesSource() {
        return messagesSource;
    }

    public MessagesSourceState getState() {
        return state;
    }

    public void destroy() {
        destroyed = true;
        service.shutdownNow();
    }

    private void setState(MessagesSourceState nState) {
        if (destroyed) {
            return;
        }
        state = nState;
        preferences.edit().putString("state", state.name()).commit();
        messagesSource.invalidateState();
        Logger.d(TAG, "Messages state: " + nState);
    }

    public void startSyncIfRequired() {
        if (state == MessagesSourceState.UNSYNCED) {
            startSync();
        }
    }

    public void startSync() {
        if (destroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != MessagesSourceState.FULL_SYNC && state != MessagesSourceState.LOAD_MORE) {
                synchronized (stateSync) {
                    setState(MessagesSourceState.FULL_SYNC);
                }
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            boolean isCompleted = false;
                            try {
                                while (application.isLoggedIn()) {
                                    try {
                                        isCompleted = requestLoad(0);
                                        application.getResponsibility().waitForResume();
                                        messagesSource.invalidateDataIfRequired();
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } finally {
                                synchronized (stateSync) {
                                    if (state == MessagesSourceState.FULL_SYNC) {
                                        if (isCompleted) {
                                            setState(MessagesSourceState.COMPLETED);
                                        } else {
                                            setState(MessagesSourceState.SYNCED);
                                        }
                                    }
                                }
                            }
                            setState(MessagesSourceState.UNSYNCED);
                        } finally {
                            Logger.d(TAG, "Messages sync time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }

    public void requestLoadMore(final int offset) {
        if (destroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != MessagesSourceState.FULL_SYNC && state != MessagesSourceState.LOAD_MORE
                    && state != MessagesSourceState.COMPLETED) {
                synchronized (stateSync) {
                    setState(MessagesSourceState.LOAD_MORE);
                }
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            try {
                                application.getResponsibility().waitForResume();
                                boolean isCompleted = requestLoad(offset);
                                application.getResponsibility().waitForResume();
                                messagesSource.invalidateDataIfRequired();
                                synchronized (stateSync) {
                                    if (state == MessagesSourceState.LOAD_MORE) {
                                        if (isCompleted) {
                                            setState(MessagesSourceState.COMPLETED);
                                        } else {
                                            setState(MessagesSourceState.SYNCED);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                synchronized (stateSync) {
                                    if (state == MessagesSourceState.LOAD_MORE) {
                                        setState(MessagesSourceState.LOAD_MORE_ERROR);
                                    }
                                }
                            }
                        } finally {
                            Logger.d(TAG, "Messages load more: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }

    public boolean isCompleted() {
        return state == MessagesSourceState.COMPLETED;
    }

    private boolean requestLoad(int offset) throws Exception {
        if (peerType == PeerType.PEER_USER_ENCRYPTED) {
            return true;
        }
        application.getMonitor().waitForConnection();
        Logger.d(TAG, "Load more messages, offset: " + offset);
        TLAbsInputPeer peer;
        if (peerType == PeerType.PEER_USER) {
            User user = application.getEngine().getUser(peerId);
            if (user == null) {
                peer = new TLInputPeerContact(peerId);
            } else {
                peer = new TLInputPeerForeign(peerId, user.getAccessHash());
            }
        } else {
            peer = new TLInputPeerChat(peerId);
        }

        TLAbsMessages messagesEx = application.getApi().doRpcCall(new TLRequestMessagesGetHistory(peer, offset, 0, PAGE_SIZE));

        application.getEngine().onLoadMoreMessages(
                messagesEx.getMessages(),
                messagesEx.getUsers(),
                messagesEx.getChats());

        messagesSource.invalidateDataIfRequired();

        if (messagesEx instanceof TLMessages) {
            return true;
        } else {
            return messagesEx.getMessages().size() == 0 || ((TLMessagesSlice) messagesEx).getCount() <= offset + PAGE_SIZE;
        }
    }
}