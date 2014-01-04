package org.telegram.android.core.engines;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.log.Logger;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class MessagesDatabase {

    private static final String TAG = "MessagesDatabase";

    private RuntimeExceptionDao<ChatMessage, Long> messageDao;

    public MessagesDatabase(ModelEngine engine) {
        this.messageDao = engine.getMessagesDao();
    }

    public ChatMessage[] getMessagesByMid(int[] mid) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
            Object[] mids = new Object[mid.length];
            for (int i = 0; i < mids.length; i++) {
                mids[i] = mid[i];
            }
            queryBuilder.where().in("mid", mids);
            return queryBuilder.query().toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new ChatMessage[0];
    }

    public ChatMessage getMessageById(int localId) {
        return messageDao.queryForId((long) localId);
    }

    public ChatMessage getMessageByMid(int mid) {
        List<ChatMessage> res = messageDao.queryForEq("mid", mid);
        if (res.size() == 0) {
            return null;
        } else {
            return res.get(0);
        }
    }

    public ChatMessage getMessageByRid(long rid) {
        List<ChatMessage> res = messageDao.queryForEq("randomId", rid);
        if (res.size() == 0) {
            return null;
        } else {
            return res.get(0);
        }
    }

    public void create(ChatMessage message) {
        messageDao.create(message);
    }

    public void update(ChatMessage message) {
        messageDao.update(message);
    }
}
