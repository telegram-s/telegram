package org.telegram.android.core.engines;

import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.MessageState;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.log.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class MessagesDatabase {

    private static final String TAG = "MessagesDatabase";

    private RuntimeExceptionDao<ChatMessage, Long> messageDao;

    public MessagesDatabase(ModelEngine engine) {
        this.messageDao = engine.getDatabase().getMessagesDao();
    }

    private long uniq(int peerType, int peerId) {
        return peerId * 10L + peerType;
    }

    public ChatMessage[] queryMessages(int peerType, int peerId, int pageSize, int offset) {
        PreparedQuery<ChatMessage> query;
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
            queryBuilder.orderByRaw("-(date * 1000000 + abs(mid))");
            queryBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType).and().eq("deletedLocal", false);
            queryBuilder.offset(offset);
            queryBuilder.limit(pageSize);
            query = queryBuilder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatMessage[0];
        }
        return messageDao.query(query).toArray(new ChatMessage[0]);
    }

    public ChatMessage[] queryUnreadedMessages(int peerType, int peerId, int pageSize, int mid) {
        ArrayList<ChatMessage> resultMessages = new ArrayList<ChatMessage>();
        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            ChatMessage message = getMessageByMid(mid);
            if (message != null) {
                PreparedQuery<ChatMessage> query;
                try {
                    QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
                    queryBuilder.orderByRaw("-(date * 1000000 + abs(mid))");
                    queryBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType).and().eq("deletedLocal", false)
                            .and().raw("(date * 1000000 + abs(mid)) >= " + (message.getDate() * 1000000L + Math.abs(message.getMid())));
                    query = queryBuilder.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ChatMessage[0];
                }
                resultMessages.addAll(messageDao.query(query));

                try {
                    QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
                    queryBuilder.orderByRaw("-(date * 1000000 + abs(mid))");
                    queryBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType).and().eq("deletedLocal", false)
                            .and().raw("(date * 1000000 + abs(mid)) <= " + (message.getDate() * 1000000L + Math.abs(message.getMid())));
                    queryBuilder.limit(pageSize);
                    query = queryBuilder.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ChatMessage[0];
                }
                resultMessages.addAll(messageDao.query(query));

                return resultMessages.toArray(new ChatMessage[0]);
            }
        }

        return null;
    }

    public ChatMessage[] getUnreadSecret(int chatId, int maxDate) {
        QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
        try {
            queryBuilder.where()
                    .eq("peerType", PeerType.PEER_USER_ENCRYPTED).and()
                    .eq("peerId", chatId).and()
                    .eq("isOut", true).and()
                    .eq("state", MessageState.SENT).and()
                    .gt("messageTimeout", 0)
                    .le("date", maxDate);

            return messageDao.query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ChatMessage[0];
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

    public void delete(ChatMessage message) {
        messageDao.create(message);
    }

    public void update(ChatMessage message) {
        messageDao.update(message);
    }

    public void diffInTx(final Iterable<ChatMessage> newMessages, final Iterable<ChatMessage> updatedMessages) {
        messageDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (ChatMessage msg : updatedMessages) {
                    messageDao.update(msg);
                }
                for (ChatMessage msg : newMessages) {
                    messageDao.create(msg);
                }
                return null;
            }
        });
    }

    public void updateInTx(final Iterable<ChatMessage> messages) {
        messageDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (ChatMessage msg : messages) {
                    messageDao.update(msg);
                }
                return null;
            }
        });
    }

    public void updateInTx(final ChatMessage... messages) {
        messageDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (ChatMessage msg : messages) {
                    messageDao.update(msg);
                }
                return null;
            }
        });
    }

    public void createInTx(final ChatMessage... messages) {
        messageDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (ChatMessage msg : messages) {
                    messageDao.create(msg);
                }
                return null;
            }
        });
    }

    public void createInTx(final Iterable<ChatMessage> messages) {
        messageDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (ChatMessage msg : messages) {
                    messageDao.create(msg);
                }
                return null;
            }
        });
    }

    public void deleteHistory(int peerType, int peerId) {
        try {
            DeleteBuilder<ChatMessage, Long> deleteBuilder = messageDao.deleteBuilder();
            deleteBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType);
            messageDao.delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ChatMessage findTopMessage(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> builder = messageDao.queryBuilder();
            builder.where().eq("deletedLocal", false).and().eq("peerType", peerType).and().eq("peerId", peerId);
            builder.orderBy("date", false);
            return messageDao.queryForFirst(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ChatMessage[] findDiedMessages(int currentTime) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
            queryBuilder.where().le("messageDieTime", currentTime).and().ne("messageDieTime", 0);
            return messageDao.query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public ChatMessage[] findPendingSelfDestructMessages(int currentTime) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
            queryBuilder.where().gt("messageDieTime", currentTime).and().ne("messageDieTime", 0);
            return messageDao.query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public ChatMessage[] findUnreadedSelfDestructMessages(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = messageDao.queryBuilder();
            queryBuilder.where().ne("messageTimeout", 0).and().eq("messageDieTime", 0).and().eq("peerType", peerType).and().eq("peerId", peerId);
            return messageDao.query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public int getMaxDateInDialog(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> builder = messageDao.queryBuilder();
            builder.where().eq("peerType", peerType).and().eq("peerId", peerId).and().eq("isOut", false);
            builder.orderBy("date", false);
            ChatMessage message = messageDao.queryForFirst(builder.prepare());
            if (message != null) {
                return message.getDate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getMaxMidInDialog(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> builder = messageDao.queryBuilder();
            builder.where().eq("peerType", peerType).and().eq("peerId", peerId).and().eq("isOut", false);
            builder.orderBy("mid", false);
            ChatMessage message = messageDao.queryForFirst(builder.prepare());
            if (message != null) {
                return message.getMid();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getMinMid() {
        try {
            ChatMessage msg = messageDao.queryForFirst(messageDao.queryBuilder().orderBy("mid", true).prepare());
            if (msg != null) {
                return Math.min(msg.getMid(), 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getMaxDate() {
        try {
            ChatMessage msg = messageDao.queryForFirst(messageDao.queryBuilder().orderBy("date", false).prepare());
            if (msg != null) {
                return msg.getDate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
