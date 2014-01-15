package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.telegram.android.Configuration;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.engines.SyncStateEngine;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalContact;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLAbsInputPeer;
import org.telegram.api.TLInputPeerChat;
import org.telegram.api.TLInputPeerContact;
import org.telegram.api.TLInputPeerForeign;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLMessages;
import org.telegram.api.messages.TLMessagesSlice;
import org.telegram.api.requests.TLRequestMessagesGetHistory;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 0:42
 */
public class MessageSource {
    private static final int STATE_UNLOADED = 0;
    private static final int STATE_LOADED = 1;
    private static final int STATE_COMPLETED = 2;

    public static final int PAGE_SIZE = 40;
    public static final int PAGE_SIZE_REMOTE = 20;

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

    private ViewSource<MessageWireframe, ChatMessage> messagesSource;

    private boolean destroyed;

    private int persistenceState = STATE_UNLOADED;

    private final String TAG;

    private SyncStateEngine syncStateEngine;

    public MessageSource(int _peerType, int _peerId, StelsApplication _application) {
        TAG = "MessageSource#" + _peerType + "@" + _peerId;
        this.peerType = _peerType;
        this.peerId = _peerId;
        this.application = _application;
        this.destroyed = false;
        this.syncStateEngine = application.getEngine().getSyncStateEngine();

        this.persistenceState = syncStateEngine.getMessagesSyncState(peerType, peerId, STATE_UNLOADED);
        if (persistenceState == STATE_COMPLETED) {
            this.state = MessagesSourceState.COMPLETED;
        } else if (persistenceState == STATE_LOADED) {
            this.state = MessagesSourceState.SYNCED;
        } else {
            this.state = MessagesSourceState.UNSYNCED;
        }

        this.messagesSource = new ViewSource<MessageWireframe, ChatMessage>() {

            @Override
            protected ChatMessage[] loadItems(int offset) {
                ChatMessage[] res = null;

                if (offset == 0) {
                    if (peerType != PeerType.PEER_USER_ENCRYPTED) {
                        DialogDescription description = application.getEngine().getDescriptionForPeer(peerType, peerId);
                        if (description != null) {
                            int unreadMid = (int) description.getFirstUnreadMessage();
                            if (unreadMid != 0) {
                                res = application.getEngine().getMessagesEngine().queryUnreadedMessages(peerType, peerId, PAGE_SIZE, unreadMid);
                            }
                        }
                    }
                }

                if (res == null) {
                    if (offset < PAGE_OVERLAP) {
                        offset = 0;
                    } else {
                        offset -= PAGE_OVERLAP;
                    }

                    res = application.getEngine().getMessagesEngine().queryMessages(peerType, peerId, PAGE_SIZE, offset);
                }

                HashSet<Integer> uids = new HashSet<Integer>();
                for (ChatMessage msg : res) {
                    if (msg.getSenderId() > 0) {
                        uids.add(msg.getSenderId());
                    }
                }
                application.getEngine().getUsersEngine().getUsersById(uids.toArray());

                return res;
            }

            @Override
            protected long getSortingKey(MessageWireframe obj) {
                if (obj.message.getMid() <= 0) {
                    return obj.message.getDate() * 1000000L + 999999L;
                } else {
                    return obj.message.getDate() * 1000000L + Math.abs(obj.message.getMid());
                }
            }

            @Override
            protected long getItemKey(MessageWireframe obj) {
                return obj.message.getDatabaseId();
            }

            @Override
            protected long getItemKeyV(ChatMessage obj) {
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

            @Override
            protected MessageWireframe convert(ChatMessage item) {
                MessageWireframe res = new MessageWireframe();
                res.message = item;

                res.peerId = item.getPeerId();
                res.peerType = item.getPeerType();
                res.mid = item.getMid();
                res.databaseId = item.getDatabaseId();
                res.randomId = item.getRandomId();
                res.dateValue = item.getDate();
                res.date = TextUtil.formatTime(item.getDate(), application);

                res.senderUser = application.getEngine().getUser(item.getSenderId());
                if (res.message.isForwarded()) {
                    res.forwardUser = application.getEngine().getUser(item.getForwardSenderId());
                }

                if (item.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                    int uid = ((TLLocalContact) item.getExtras()).getUserId();
                    res.relatedUser = application.getEngine().getUser(uid);
                }

//                if (item.getRawContentType() == ContentType.MESSAGE_TEXT) {
//                    res.cachedLayout = MessageView.prepareLayout(res, application);
//                }

                return res;
            }
        };
    }

    public ViewSource<MessageWireframe, ChatMessage> getMessagesSource() {
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
        long start = SystemClock.uptimeMillis();
        state = nState;
        // preferences.edit().putString("state", state.name()).commit();
        messagesSource.invalidateState();
        Logger.d(TAG, "Messages state: " + nState + " in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    private void onCompleted() {
        if (destroyed) {
            return;
        }
        persistenceState = STATE_COMPLETED;
        syncStateEngine.setMessagesSyncState(peerType, peerId, persistenceState);
    }

    private void onLoaded() {
        if (destroyed) {
            return;
        }
        persistenceState = STATE_LOADED;
        syncStateEngine.setMessagesSyncState(peerType, peerId, persistenceState);
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
                                            onCompleted();
                                        } else {
                                            setState(MessagesSourceState.SYNCED);
                                            onLoaded();
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
                                            onCompleted();
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
        long startRequest = SystemClock.uptimeMillis();
        TLAbsMessages messagesEx = application.getApi().doRpcCall(new TLRequestMessagesGetHistory(peer, offset, 0, PAGE_SIZE_REMOTE));
        Logger.d(TAG, "Requested in " + (SystemClock.uptimeMillis() - startRequest) + " ms");

        long start = SystemClock.uptimeMillis();
        application.getEngine().onUsers(messagesEx.getUsers());
        application.getEngine().getGroupsEngine().onGroupsUpdated(messagesEx.getChats());
        application.getEngine().onLoadMoreMessages(messagesEx.getMessages());

        Logger.d(TAG, "Save to database in " + (SystemClock.uptimeMillis() - start) + " ms");

        messagesSource.invalidateDataIfRequired();

        if (messagesEx instanceof TLMessages) {
            return true;
        } else {
            return messagesEx.getMessages().size() == 0 || ((TLMessagesSlice) messagesEx).getCount() <= offset + PAGE_SIZE_REMOTE;
        }
    }
}