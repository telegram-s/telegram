package org.telegram.android.core.engines;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.core.model.service.TLLocalActionChatDeleteUser;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsMessage;
import org.telegram.api.TLDialog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class MessagesEngine {

    private static final String TAG = "MessagesEngine";

    private ModelEngine engine;
    private MessagesDatabase database;
    private StelsApplication application;

    private int maxDate = 0;
    private AtomicInteger minMid = null;
    private Object minMidSync = new Object();

    public MessagesEngine(ModelEngine engine) {
        this.engine = engine;
        this.database = new MessagesDatabase(engine);
        this.application = engine.getApplication();
    }

    public synchronized ChatMessage[] getUnsentMessages() {
        return database.getUnsentMessages();
    }

    public synchronized ChatMessage[] getUnsyncedDeletedMessages() {
        return database.getUnsyncedDeletedMessages();
    }

    public synchronized ChatMessage[] getUnsyncedRestoredMessages() {
        return database.getUnsyncedRestoredMessages();
    }

    public synchronized ChatMessage[] queryMessages(int peerType, int peerId, int pageSize, int offset) {
        return database.queryMessages(peerType, peerId, pageSize, offset);
    }

    public synchronized ChatMessage[] queryUnreadedMessages(int peerType, int peerId, int pageSize, int mid) {
        return database.queryUnreadedMessages(peerType, peerId, pageSize, mid);
    }

    public synchronized ChatMessage getMessageByMid(int mid) {
        return database.getMessageByMid(mid);
    }

    public synchronized ChatMessage[] getMessagesByMid(int[] mids) {
        return database.getMessagesByMid(mids);
    }

    public synchronized ChatMessage getMessageById(int id) {
        return database.getMessageById(id);
    }

    public synchronized ChatMessage getMessageByRandomId(long rid) {
        return database.getMessageByRid(rid);
    }

    public synchronized void create(ChatMessage message) {
        database.create(message);
        application.getDataSourceKernel().onSourceAddMessage(message);
        updateMaxDate(message);
    }

    public synchronized void delete(ChatMessage message) {
        database.delete(message);
        application.getDataSourceKernel().onSourceRemoveMessage(message);
        updateMaxDate(message);
    }

    public synchronized void update(ChatMessage message) {
        database.update(message);
        application.getDataSourceKernel().onSourceUpdateMessage(message);
        updateMaxDate(message);
    }

    public synchronized void deleteHistory(int peerType, int peerId) {
        database.deleteHistory(peerType, peerId);
        application.getDataSourceKernel().removeMessageSource(peerType, peerId);
    }

    public synchronized ChatMessage findTopMessage(int peerType, int peerId) {
        return database.findTopMessage(peerType, peerId);
    }

    public synchronized ChatMessage[] findDiedMessages(int currentTime) {
        return database.findDiedMessages(currentTime);
    }

    public synchronized ChatMessage[] findPendingSelfDestructMessages(int currentTime) {
        return database.findPendingSelfDestructMessages(currentTime);
    }

    public synchronized ChatMessage[] findUnreadedSelfDestructMessages(int peerType, int peerId) {
        return database.findUnreadedSelfDestructMessages(peerType, peerId);
    }

    public synchronized int getMaxDateInDialog(int peerType, int peerId) {
        return database.getMaxDateInDialog(peerType, peerId);
    }

    public synchronized int getMaxMidInDialog(int peerType, int peerId) {
        return database.getMaxMidInDialog(peerType, peerId);
    }

    public synchronized ChatMessage[] getUnreadSecret(int chatId, int maxDate) {
        return database.getUnreadSecret(chatId, maxDate);
    }

    public synchronized void onDeletedOnServer(int[] mids) {
        ChatMessage[] messages = getMessagesByMid(mids);
        for (ChatMessage msg : messages) {
            msg.setDeletedServer(true);
            msg.setDeletedLocal(true);
            database.update(msg);
            application.getDataSourceKernel().onSourceRemoveMessage(msg);
        }
    }

    public synchronized void onRestoredOnServer(int[] mids) {
        ChatMessage[] messages = getMessagesByMid(mids);
        for (ChatMessage msg : messages) {
            msg.setDeletedServer(false);
            msg.setDeletedLocal(false);
            database.update(msg);
            application.getDataSourceKernel().onSourceAddMessage(msg);
        }
    }

    private void updateMaxDate(ChatMessage msg) {
        if (msg.getDate() > maxDate) {
            maxDate = msg.getDate();
        }
    }

    public int getMaxDate() {
        if (maxDate == 0) {
            maxDate = database.getMaxDate();
        }
        return maxDate;
    }

    public int generateMid() {
        if (minMid == null) {
            synchronized (minMidSync) {
                minMid = new AtomicInteger(database.getMinMid());
            }
        }
        return minMid.decrementAndGet();
    }

    public synchronized void onMessageRead(ChatMessage[] messages) {
        for (ChatMessage msg : messages) {
            msg.setState(MessageState.READED);
        }
        database.updateInTx(messages);
    }

    public synchronized ChatMessage[] updateMessages(List<TLAbsMessage> messages) {
        long start = SystemClock.uptimeMillis();
        ChatMessage[] converted = convert(messages);
        ArrayList<ChatMessage>[] diff = buildDiff(converted);
        Logger.d(TAG, "updateMessages:prepare time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        database.diffInTx(diff[0], diff[1]);
        Logger.d(TAG, "updateMessages:update time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        application.getDataSourceKernel().onSourceUpdateMessages(diff[0].toArray(new ChatMessage[0]));
        application.getDataSourceKernel().onSourceAddMessages(diff[1].toArray(new ChatMessage[0]));
        Logger.d(TAG, "updateMessages:datasource time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        Logger.d(TAG, "updateMessages:complete time: " + (SystemClock.uptimeMillis() - start));
        for (ChatMessage message : diff[0]) {
            if (message.getRawContentType() == ContentType.MESSAGE_PHOTO) {
                engine.getMediaEngine().saveMedia(message.getMid(), message);
            } else if (message.getRawContentType() == ContentType.MESSAGE_VIDEO) {
                engine.getMediaEngine().saveMedia(message.getMid(), message);
            }
        }

        return converted;
    }

    private ArrayList<ChatMessage>[] buildDiff(ChatMessage[] src) {
        int[] ids = new int[src.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = src[i].getMid();
        }
        ChatMessage[] original = getMessagesByMid(ids);

        ArrayList<ChatMessage> newMessages = new ArrayList<ChatMessage>();
        ArrayList<ChatMessage> updatedMessages = new ArrayList<ChatMessage>();

        for (ChatMessage m : src) {
            ChatMessage orig = EngineUtils.searchMessage(original, m.getMid());
            if (orig != null) {
                m.setDatabaseId(orig.getDatabaseId());
                updatedMessages.add(m);
            } else {
                newMessages.add(m);
            }
        }
        return new ArrayList[]{newMessages, updatedMessages};
    }

    private ChatMessage[] convert(List<TLAbsMessage> messages) {
        ChatMessage[] converted = new ChatMessage[messages.size()];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = EngineUtils.fromTlMessage(messages.get(i), application);
        }
        return converted;
    }
}
