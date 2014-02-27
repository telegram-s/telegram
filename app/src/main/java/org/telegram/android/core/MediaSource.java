package org.telegram.android.core;

import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.ui.source.ViewSourceState;

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

    public MediaSource(int _peerType, int _peerId, TelegramApplication _application) {
        this.application = _application;
        this.peerType = _peerType;
        this.peerId = _peerId;

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
            protected MediaRecord convert(MediaRecord item) {
                return item;
            }
        };
        mediaSource.invalidateState();
    }

    public ViewSource<MediaRecord, MediaRecord> getSource() {
        return mediaSource;
    }
}
