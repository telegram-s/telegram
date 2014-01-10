package org.telegram.android.core.engines;

import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import org.telegram.android.core.model.*;
import org.telegram.android.log.Logger;
import org.telegram.dao.Message;
import org.telegram.dao.MessageDao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class MessagesDatabase {

    private static final String TAG = "MessagesDatabase";

    private MessageDao messageDao;

    public MessagesDatabase(ModelEngine engine) {
        this.messageDao = engine.getDaoSession().getMessageDao();
    }

    private long uniq(int peerType, int peerId) {
        return peerId * 10L + peerType;
    }

    public ChatMessage[] getUnsentMessages() {
        return convert(messageDao.queryBuilder()
                .where(
                        MessageDao.Properties.State.eq(MessageState.PENDING),
                        MessageDao.Properties.IsOut.eq(true),
                        MessageDao.Properties.DeletedLocal.eq(false))
                .list());
    }

    public ChatMessage[] getUnsyncedDeletedMessages() {
        return convert(messageDao.queryBuilder()
                .where(MessageDao.Properties.DeletedLocal.eq(true),
                        MessageDao.Properties.DeletedServer.eq(false))
                .list());
    }

    public ChatMessage[] getUnsyncedRestoredMessages() {
        return convert(messageDao.queryBuilder()
                .where(MessageDao.Properties.DeletedLocal.eq(false),
                        MessageDao.Properties.DeletedServer.eq(true))
                .list());
    }

    public ChatMessage[] queryMessages(int peerType, int peerId, int pageSize, int offset) {
        List<Message> dbres = messageDao.queryBuilder()
                .where(
                        MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                        MessageDao.Properties.DeletedLocal.eq(false))
                .orderRaw("-(" + MessageDao.Properties.Date.columnName + " * 1000000 + abs(" + MessageDao.Properties.Mid.columnName + "))")
                .offset(offset)
                .limit(pageSize)
                .list();

        ChatMessage[] res = new ChatMessage[dbres.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = convert(dbres.get(i));
        }

        return res;
    }

    public ChatMessage[] queryUnreadedMessages(int peerType, int peerId, int pageSize, int mid) {
        ArrayList<ChatMessage> resultMessages = new ArrayList<ChatMessage>();
        if (peerType != PeerType.PEER_USER_ENCRYPTED) {
            ChatMessage message = getMessageByMid(mid);
            if (message != null) {
                List<Message> msgs = messageDao.queryBuilder()
                        .where(MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                                MessageDao.Properties.DeletedLocal.eq(false),
                                new WhereCondition.StringCondition(
                                        "(date * 1000000 + abs(mid)) >= " + (message.getDate() * 1000000L + Math.abs(message.getMid()))))
                        .orderRaw("-(date * 1000000 + abs(mid))")
                        .list();

                for (Message m : msgs) {
                    resultMessages.add(convert(m));
                }

                msgs = messageDao.queryBuilder()
                        .where(MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                                MessageDao.Properties.DeletedLocal.eq(false),
                                new WhereCondition.StringCondition("(date * 1000000 + abs(mid)) <= " + (message.getDate() * 1000000L + Math.abs(message.getMid()))))
                        .orderRaw("-(date * 1000000 + abs(mid))")
                        .limit(pageSize)
                        .list();

                for (Message m : msgs) {
                    resultMessages.add(convert(m));
                }

                return resultMessages.toArray(new ChatMessage[0]);
            }
        }

        return null;
    }

    public ChatMessage[] getUnreadSecret(int chatId, int maxDate) {
        List<Message> msg = messageDao.queryBuilder()
                .where(MessageDao.Properties.PeerUniqId.eq(uniq(PeerType.PEER_USER_ENCRYPTED, chatId)),
                        MessageDao.Properties.IsOut.eq(true),
                        MessageDao.Properties.State.eq(MessageState.SENT),
                        MessageDao.Properties.MessageTimeout.gt(0),
                        MessageDao.Properties.Date.le(maxDate))
                .list();

        ChatMessage[] res = new ChatMessage[msg.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = convert(msg.get(i));
        }
        return res;
    }

    public ChatMessage[] getMessagesByMid(int[] mid) {
        Object[] mids = new Object[mid.length];
        for (int i = 0; i < mids.length; i++) {
            mids[i] = mid[i];
        }
        Message[] dbRes = messageDao.queryBuilder().where(MessageDao.Properties.Mid.in((Object[]) mids)).list().toArray(new Message[0]);
        ChatMessage[] res = new ChatMessage[dbRes.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = convert(dbRes[i]);
        }

        return res;
    }

    public ChatMessage getMessageById(int localId) {
        return convert(messageDao.load((long) localId));
    }

    public ChatMessage getMessageByMid(int mid) {
        List<Message> res = messageDao.queryBuilder().where(MessageDao.Properties.Mid.eq(mid)).list();
        if (res.size() == 0) {
            return null;
        } else {
            return convert(res.get(0));
        }
    }

    public ChatMessage getMessageByRid(long rid) {
        List<Message> res = messageDao.queryBuilder().where(MessageDao.Properties.Rid.eq(rid)).list();
        if (res.size() == 0) {
            return null;
        } else {
            return convert(res.get(0));
        }
    }

    public void create(ChatMessage message) {
        Message msg = convert(message);
        messageDao.insert(msg);
        message.setDatabaseId((int) (long) msg.getId());
    }

    public void delete(ChatMessage message) {
        messageDao.delete(convert(message));
    }

    public void update(ChatMessage message) {
        messageDao.update(convert(message));
    }

    public void diffInTx(final List<ChatMessage> newMessages, final List<ChatMessage> updatedMessages) {
        if (newMessages.size() > 0) {
            Message[] nMessages = new Message[newMessages.size()];
            for (int i = 0; i < nMessages.length; i++) {
                nMessages[i] = convert(newMessages.get(i));
                nMessages[i].setId(null);
            }
            messageDao.insertInTx(nMessages);
            for (int i = 0; i < nMessages.length; i++) {
                newMessages.get(i).setDatabaseId((int) (long) nMessages[i].getId());
            }
        }
        if (updatedMessages.size() > 0) {
            Message[] nMessages = new Message[updatedMessages.size()];
            for (int i = 0; i < nMessages.length; i++) {
                nMessages[i] = convert(updatedMessages.get(i));
            }
            messageDao.updateInTx(nMessages);
        }
    }

    public void updateInTx(final ChatMessage... messages) {
        Message[] nMessages = new Message[messages.length];
        for (int i = 0; i < nMessages.length; i++) {
            nMessages[i] = convert(messages[i]);
        }
        messageDao.updateInTx(nMessages);
    }

    public void deleteHistory(int peerType, int peerId) {
        messageDao.queryBuilder().where(MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId))).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public ChatMessage findTopMessage(int peerType, int peerId) {
        List<Message> res = messageDao.queryBuilder().where(MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)))
                .orderDesc(MessageDao.Properties.Date)
                .limit(1)
                .list();

        if (res.size() > 0) {
            return convert(res.get(0));
        } else {
            return null;
        }
    }

    public ChatMessage[] findDiedMessages(int currentTime) {
        return convert(messageDao.queryBuilder().where(
                MessageDao.Properties.MessageDieTime.le(currentTime),
                MessageDao.Properties.MessageTimeout.notEq(0))
                .list());
    }

    public ChatMessage[] findPendingSelfDestructMessages(int currentTime) {
        return convert(messageDao.queryBuilder().where(
                MessageDao.Properties.MessageDieTime.gt(currentTime),
                MessageDao.Properties.MessageTimeout.notEq(0))
                .list());
    }

    public ChatMessage[] findUnreadedSelfDestructMessages(int peerType, int peerId) {
        return convert(messageDao.queryBuilder().where(
                MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                MessageDao.Properties.MessageDieTime.eq(0),
                MessageDao.Properties.MessageTimeout.notEq(0))
                .list());
    }

    public int getMaxDateInDialog(int peerType, int peerId) {
        List<Message> messages = messageDao.queryBuilder()
                .where(
                        MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                        MessageDao.Properties.IsOut.eq(false))
                .orderDesc(MessageDao.Properties.Date)
                .limit(1)
                .list();

        if (messages.size() > 0) {
            return messages.get(0).getDate();
        } else {
            return 0;
        }
    }

    public int getMaxMidInDialog(int peerType, int peerId) {
        List<Message> messages = messageDao.queryBuilder()
                .where(
                        MessageDao.Properties.PeerUniqId.eq(uniq(peerType, peerId)),
                        MessageDao.Properties.IsOut.eq(false))
                .orderDesc(MessageDao.Properties.Mid)
                .limit(1)
                .list();

        if (messages.size() > 0) {
            return messages.get(0).getMid();
        } else {
            return 0;
        }
    }

    public int getMinMid() {
        List<Message> res = messageDao.queryBuilder().orderAsc(MessageDao.Properties.Mid).limit(1).list();
        if (res.size() > 0) {
            return Math.min(res.get(0).getMid(), 0);
        } else {
            return 0;
        }
    }

    public int getMaxDate() {
        List<Message> res = messageDao.queryBuilder().orderAsc(MessageDao.Properties.Date).limit(1).list();
        if (res.size() > 0) {
            return res.get(0).getDate();
        } else {
            return 0;
        }
    }

    private ChatMessage convert(Message src) {
        ChatMessage res = new ChatMessage();
        res.setDatabaseId((int) (long) src.getId());
        res.setPeerId((int) (src.getPeerUniqId() / 10L));
        res.setPeerType((int) (src.getPeerUniqId() % 10L));
        res.setMid(src.getMid());
        res.setRandomId(src.getRid());
        res.setDate(src.getDate());
        res.setState(src.getState());
        res.setSenderId(src.getSenderId());
        res.setContentType(src.getContentType());
        res.setMessage(src.getMessage());
        res.setOut(src.getIsOut());
        if (src.getExtras() != null) {
            try {
                res.setExtras(TLLocalContext.getInstance().deserializeMessage(src.getExtras()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        res.setDeletedLocal(src.getDeletedLocal());
        res.setDeletedServer(src.getDeletedServer());
        res.setForwardMid(src.getForwardMid());
        res.setForwardDate(src.getForwardDate());
        res.setForwardSenderId(src.getForwardSenderId());
        res.setMessageDieTime(src.getMessageDieTime());
        res.setMessageTimeout(src.getMessageTimeout());
        return res;
    }


    private Message convert(ChatMessage src) {
        Message res = new Message();
        if (src.getDatabaseId() != 0) {
            res.setId((long) src.getDatabaseId());
        }
        res.setPeerUniqId(uniq(src.getPeerType(), src.getPeerId()));
        res.setMid(src.getMid());
        res.setRid(src.getRandomId());
        res.setDate(src.getDate());
        res.setState(src.getState());
        res.setSenderId(src.getSenderId());
        res.setContentType(src.getContentType());
        res.setMessage(src.getMessage());
        res.setIsOut(src.isOut());
        if (src.getExtras() != null) {
            try {
                res.setExtras(src.getExtras().serialize());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        res.setDeletedLocal(src.isDeletedLocal());
        res.setDeletedServer(src.isDeletedServer());
        res.setForwardMid(src.getForwardMid());
        res.setForwardDate(src.getForwardDate());
        res.setForwardSenderId(src.getForwardSenderId());
        res.setMessageDieTime(src.getMessageDieTime());
        res.setMessageTimeout(src.getMessageTimeout());
        return res;
    }

    private ChatMessage[] convert(List<Message> dbRes) {
        ChatMessage[] res = new ChatMessage[dbRes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = convert(dbRes.get(i));
        }
        return res;
    }
}
