package org.telegram.android.core.engines;

import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.ContentType;
import org.telegram.android.core.model.MediaRecord;

import java.util.List;

/**
 * Created by ex3ndr on 17.12.13.
 */
public class MediaEngine {
    private MediaDatabase mediaDatabase;
    private TelegramApplication application;

    public MediaEngine(ModelEngine engine) {
        this.mediaDatabase = new MediaDatabase(engine);
        this.application = engine.getApplication();
    }

    public synchronized int getMediaCount(int peerType, int peerId) {
        return mediaDatabase.getMediaCount(peerType, peerId);
    }

    public synchronized MediaRecord findMedia(int mid) {
        return mediaDatabase.loadMedia(mid);
    }

    public synchronized void saveMedia(ChatMessage[] sourceMessages) {
        for (ChatMessage msg : sourceMessages) {
            saveMedia(msg);
        }
    }

    public synchronized MediaRecord[] queryMedia(int peerType, int peerId, int offset, int limit) {
        return mediaDatabase.queryMedia(peerType, peerId, offset, limit);
    }

    public synchronized MediaRecord[] queryMedia(int peerType, int peerId) {
        return mediaDatabase.queryMedia(peerType, peerId);
    }

    public synchronized boolean saveMedia(ChatMessage sourceMessage) {
        if (sourceMessage.getRawContentType() != ContentType.MESSAGE_PHOTO
                && sourceMessage.getRawContentType() != ContentType.MESSAGE_VIDEO) {
            return false;
        }

        MediaRecord record = findMedia(sourceMessage.getMid());
        if (record != null)
            return false;

        record = new MediaRecord();
        record.setMid(sourceMessage.getMid());
        record.setDate(sourceMessage.getDate());
        record.setPeerId(sourceMessage.getPeerId());
        record.setPeerType(sourceMessage.getPeerType());
        if (sourceMessage.getRawContentType() == ContentType.MESSAGE_PHOTO
                || sourceMessage.getRawContentType() == ContentType.MESSAGE_VIDEO) {
            record.setPreview(sourceMessage.getExtras());
        }

        if (sourceMessage.getForwardSenderId() != 0) {
            record.setSenderId(sourceMessage.getForwardSenderId());
        } else {
            record.setSenderId(sourceMessage.getSenderId());
        }
        mediaDatabase.saveMedia(record);
        application.getDataSourceKernel().onSourceAddMedia(record);
        return true;
    }

    public synchronized void deleteMedia(int mid) {
        MediaRecord record = findMedia(mid);
        if (record != null) {
            mediaDatabase.deleteMedia(mid);
            application.getDataSourceKernel().onSourceRemoveMedia(record);
        }
    }

    public synchronized void deleteMediaFromChat(int peerType, int peerId) {
        mediaDatabase.deleteMediaFromChat(peerType, peerId);
    }

    public synchronized void clear() {
        mediaDatabase.clear();
    }
}
