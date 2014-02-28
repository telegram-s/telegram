package org.telegram.android.core;

import android.os.SystemClock;
import org.aspectj.internal.lang.annotation.ajcDeclarePrecedence;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.engines.SyncStateEngine;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsMessages;
import org.telegram.api.messages.TLMessages;
import org.telegram.api.requests.TLRequestMessagesSearch;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by ex3ndr on 27.02.14.
 */
public class MediaSource {
    private static final int STATE_UNLOADED = 0;
    private static final int STATE_LOADED = 1;
    private static final int STATE_COMPLETED = 2;

    public static final int PAGE_SIZE = 40;
    public static final int PAGE_SIZE_REMOTE = 20;

    public static final int PAGE_OVERLAP = 3;

    public static final int PAGE_REQUEST_PADDING = 20;

    private TelegramApplication application;
    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("DialogSourceService#" + res.hashCode());
            res.setPriority(Configuration.SOURCES_THREAD_PRIORITY);
            return res;
        }
    });

    private int peerType, peerId;
    private ViewSource<MediaRecord, MediaRecord> mediaSource;

    private final Object stateSync = new Object();
    private MediaSourceState state;
    private boolean destroyed = false;

    private final String TAG;

    private SyncStateEngine syncStateEngine;
    private int persistenceState;

    public MediaSource(int _peerType, int _peerId, TelegramApplication _application) {
        TAG = "MediaSource#" + _peerType + "@" + _peerId;
        this.application = _application;
        this.peerType = _peerType;
        this.peerId = _peerId;
        this.syncStateEngine = application.getEngine().getSyncStateEngine();

        this.persistenceState = syncStateEngine.getMediaSyncState(peerType, peerId, STATE_UNLOADED);
//        if (persistenceState == STATE_COMPLETED) {
//            this.state = MediaSourceState.COMPLETED;
//        } else if (persistenceState == STATE_LOADED) {
//            this.state = MediaSourceState.SYNCED;
//        } else {
//            this.state = MediaSourceState.UNSYNCED;
//        }
        this.state = MediaSourceState.UNSYNCED;

        mediaSource = new ViewSource<MediaRecord, MediaRecord>() {
            @Override
            protected MediaRecord[] loadItems(int offset) {
                if (offset < PAGE_OVERLAP) {
                    offset = 0;
                } else {
                    offset -= PAGE_OVERLAP;
                }
                return application.getEngine().getMediaEngine().queryMedia(peerType, peerId, offset, PAGE_SIZE);
            }

            @Override
            protected long getSortingKey(MediaRecord obj) {
                return obj.getDate();
            }

            @Override
            protected long getItemKey(MediaRecord obj) {
                return obj.getDatabaseId();
            }

            @Override
            protected long getItemKeyV(MediaRecord obj) {
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
                if (index > getItemsCount() - PAGE_REQUEST_PADDING && state != MediaSourceState.LOAD_MORE_ERROR) {
                    requestLoadMore(getItemsCount());
                }
            }

            @Override
            protected MediaRecord convert(MediaRecord item) {
                return item;
            }
        };
    }

    public void startSyncIfRequired() {
        if (state == MediaSourceState.UNSYNCED) {
            startSync();
        }
    }

    public ViewSource<MediaRecord, MediaRecord> getSource() {
        return mediaSource;
    }

    private void setState(MediaSourceState nState) {
        if (destroyed) {
            return;
        }
        long start = SystemClock.uptimeMillis();
        state = nState;
        mediaSource.invalidateState();
        Logger.d(TAG, "Media state: " + nState + " in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void startSync() {
        synchronized (stateSync) {
            if (state != MediaSourceState.FULL_SYNC && state != MediaSourceState.LOAD_MORE && state != MediaSourceState.COMPLETED) {
                synchronized (stateSync) {
                    setState(MediaSourceState.FULL_SYNC);
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
                                        isCompleted = loadMore(0);
                                        application.getResponsibility().waitForResume();
                                        mediaSource.invalidateDataIfRequired();
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } finally {
                                synchronized (stateSync) {
                                    if (state == MediaSourceState.FULL_SYNC) {
                                        if (isCompleted) {
                                            setState(MediaSourceState.COMPLETED);
                                            onCompleted();
                                        } else {
                                            setState(MediaSourceState.SYNCED);
                                            onLoaded();
                                        }
                                    }
                                }
                            }
                            setState(MediaSourceState.UNSYNCED);
                        } finally {
                            Logger.d(TAG, "Messages sync time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }


    public void requestLoadMore(final int offset) {
        synchronized (stateSync) {
            if (state != MediaSourceState.FULL_SYNC && state != MediaSourceState.LOAD_MORE && state != MediaSourceState.COMPLETED) {
                synchronized (stateSync) {
                    setState(MediaSourceState.LOAD_MORE);
                }
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            boolean isCompleted = loadMore(offset);
                            mediaSource.invalidateDataIfRequired();
                            synchronized (stateSync) {
                                if (isCompleted) {
                                    setState(MediaSourceState.COMPLETED);
                                } else {
                                    setState(MediaSourceState.SYNCED);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            synchronized (stateSync) {
                                if (state == MediaSourceState.LOAD_MORE) {
                                    setState(MediaSourceState.LOAD_MORE_ERROR);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private boolean loadMore(int offset) throws Exception {
        TLAbsInputPeer peer;
        if (peerType == PeerType.PEER_CHAT) {
            peer = new TLInputPeerChat(peerId);
        } else if (peerType == PeerType.PEER_USER) {
            User u = application.getEngine().getUser(peerId);
            peer = new TLInputPeerForeign(u.getUid(), u.getAccessHash());
        } else {
            //throw new UnsupportedOperationException();
            return true;
        }
        TLAbsMessages tlAbsMessages = application.getApi()
                .doRpcCall(new TLRequestMessagesSearch(
                        peer, "", new TLInputMessagesFilterPhotoVideo(), 0, 0, offset, 0, PAGE_SIZE_REMOTE));
        application.getEngine().getUsersEngine().onUsers(tlAbsMessages.getUsers());
        application.getEngine().getGroupsEngine().onGroupsUpdated(tlAbsMessages.getChats());
        if (tlAbsMessages.getMessages().size() == 0) {
            return true;
        } else {
            boolean added = false;
            for (TLAbsMessage message : tlAbsMessages.getMessages()) {
                ChatMessage msg = EngineUtils.fromTlMessage(message, application);
                added |= application.getEngine().getMediaEngine().saveMedia(msg);
            }
            application.notifyUIUpdate();
            return !added;
        }
    }

    private void onCompleted() {
        if (destroyed) {
            return;
        }
        persistenceState = STATE_COMPLETED;
        syncStateEngine.setMediaSyncState(peerType, peerId, persistenceState);
    }

    private void onLoaded() {
        if (destroyed) {
            return;
        }
        persistenceState = STATE_LOADED;
        syncStateEngine.setMediaSyncState(peerType, peerId, persistenceState);
    }
}
