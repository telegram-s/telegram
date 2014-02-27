package org.telegram.android.core.engines;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.ContentType;
import org.telegram.android.core.model.MediaRecord;

import java.util.List;

/**
 * Created by ex3ndr on 17.12.13.
 */
public class MediaEngine {
    private MediaDatabase mediaDatabase;

    public MediaEngine(ModelEngine engine) {
        this.mediaDatabase = new MediaDatabase(engine);
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

    public List<MediaRecord> lazyQueryMedia(int peerType, int peerId) {
        return mediaDatabase.lazyQueryMedia(peerType, peerId);
    }

    public synchronized MediaRecord saveMedia(ChatMessage sourceMessage) {
        if (sourceMessage.getRawContentType() != ContentType.MESSAGE_PHOTO
                && sourceMessage.getRawContentType() != ContentType.MESSAGE_VIDEO) {
            return null;
        }

        MediaRecord record = findMedia(sourceMessage.getMid());
        if (record != null)
            return record;

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
        return record;
    }

    public synchronized void deleteMedia(int mid) {
        mediaDatabase.deleteMedia(mid);
    }

    public synchronized void deleteMediaFromChat(int peerType, int peerId) {
        mediaDatabase.deleteMediaFromChat(peerType, peerId);
    }

    public synchronized void clear() {
        mediaDatabase.clear();
    }
}
