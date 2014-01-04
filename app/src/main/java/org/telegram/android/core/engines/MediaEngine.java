package org.telegram.android.core.engines;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.ContentType;
import org.telegram.android.core.model.MediaRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ex3ndr on 17.12.13.
 */
public class MediaEngine {
    private RuntimeExceptionDao<MediaRecord, Long> mediaDao;

    public MediaEngine(ModelEngine engine) {
        mediaDao = engine.getMediasDao();
    }

    public int getMediaCount(int peerType, int peerId) {
        PreparedQuery<MediaRecord> query = null;
        try {
            QueryBuilder<MediaRecord, Long> builder = mediaDao.queryBuilder();
            if (peerType >= 0) {
                builder.where().eq("peerType", peerType).and().eq("peerId", peerId);
            }
            builder.setCountOf(true);
            query = builder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) mediaDao.countOf(query);
    }

    public MediaRecord findMedia(int mid) {
        List<MediaRecord> res = mediaDao.queryForEq("mid", mid);
        if (res.size() == 0) {
            return null;
        } else {
            return res.get(0);
        }
    }

    public void saveMedia(int mid, ChatMessage sourceMessage) {
        MediaRecord record = findMedia(mid);
        if (record != null)
            return;

        record = new MediaRecord();
        record.setMid(sourceMessage.getMid());
        record.setDate(sourceMessage.getDate());
        record.setPeerId(sourceMessage.getPeerId());
        record.setPeerType(sourceMessage.getPeerType());
        if (sourceMessage.getRawContentType() == ContentType.MESSAGE_PHOTO) {
            record.setPreview(sourceMessage.getExtras());
        } else if (sourceMessage.getRawContentType() == ContentType.MESSAGE_VIDEO) {
            record.setPreview(sourceMessage.getExtras());
        }
        if (sourceMessage.getForwardSenderId() != 0) {
            record.setSenderId(sourceMessage.getForwardSenderId());
        } else {
            record.setSenderId(sourceMessage.getSenderId());
        }
        mediaDao.create(record);
    }

    public void deleteMedia(int mid) {
        MediaRecord record = findMedia(mid);
        if (record == null)
            return;
        mediaDao.delete(record);
    }

    public void deleteMediaFromChat(int peerType, int peerId) {
        try {
            DeleteBuilder<MediaRecord, Long> deleteBuilder = mediaDao.deleteBuilder();
            deleteBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType);
            mediaDao.delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
