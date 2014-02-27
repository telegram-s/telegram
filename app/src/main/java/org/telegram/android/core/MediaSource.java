package org.telegram.android.core;

import android.os.SystemClock;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.MediaRecord;
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

    public MediaSource(int _peerType, int _peerId, TelegramApplication _application) {
        TAG = "MediaSource#" + _peerType + "@" + _peerId;
        this.application = _application;
        this.peerType = _peerType;
        this.peerId = _peerId;

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
                return ViewSourceState.COMPLETED;
            }

            @Override
            protected void onItemRequested(int index) {
                if (index > getItemsCount() - PAGE_REQUEST_PADDING) {
                    requestLoadMore(getItemsCount());
                }
            }

            @Override
            protected MediaRecord convert(MediaRecord item) {
                return item;
            }
        };
        mediaSource.invalidateState();
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
        // preferences.edit().putString("state", state.name()).commit();
        mediaSource.invalidateState();
        Logger.d(TAG, "Media state: " + nState + " in " + (SystemClock.uptimeMillis() - start) + " ms");
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
                            TLAbsMessages tlAbsMessages = application.getApi()
                                    .doRpcCall(new TLRequestMessagesSearch(
                                            new TLInputPeerChat(peerId), "", new TLInputMessagesFilterPhotoVideo(), 0, 0, offset, 0, PAGE_SIZE_REMOTE));
                            if (tlAbsMessages.getMessages().size() == 0) {
                                synchronized (stateSync) {
                                    setState(MediaSourceState.COMPLETED);
                                }
                            } else {
                                for (TLAbsMessage message : tlAbsMessages.getMessages()) {
                                    ChatMessage msg = EngineUtils.fromTlMessage(message, application);
                                    MediaRecord record = application.getEngine().getMediaEngine().saveMedia(msg);
                                    if (record != null) {
                                        mediaSource.addItem(record);
                                    }
                                }
                                mediaSource.invalidateData();
                                synchronized (stateSync) {
                                    setState(MediaSourceState.SYNCED);
                                }
                            }
                        } catch (IOException e) {
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
//        if (destroyed) {
//            return;
//        }
//        synchronized (stateSync) {
//            if (state != MessagesSourceState.FULL_SYNC && state != MessagesSourceState.LOAD_MORE
//                    && state != MessagesSourceState.COMPLETED) {
//                synchronized (stateSync) {
//                    setState(MessagesSourceState.LOAD_MORE);
//                }
//                service.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        long start = SystemClock.uptimeMillis();
//                        try {
//                            try {
//                                application.getResponsibility().waitForResume();
//                                boolean isCompleted = requestLoad(offset);
//                                application.getResponsibility().waitForResume();
//                                messagesSource.invalidateDataIfRequired();
//                                synchronized (stateSync) {
//                                    if (state == MessagesSourceState.LOAD_MORE) {
//                                        if (isCompleted) {
//                                            setState(MessagesSourceState.COMPLETED);
//                                            onCompleted();
//                                        } else {
//                                            setState(MessagesSourceState.SYNCED);
//                                        }
//                                    }
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                synchronized (stateSync) {
//                                    if (state == MessagesSourceState.LOAD_MORE) {
//                                        setState(MessagesSourceState.LOAD_MORE_ERROR);
//                                    }
//                                }
//                            }
//                        } finally {
//                            Logger.d(TAG, "Messages load more: " + (SystemClock.uptimeMillis() - start) + " ms");
//                        }
//                    }
//                });
//            }
//        }
    }
}
