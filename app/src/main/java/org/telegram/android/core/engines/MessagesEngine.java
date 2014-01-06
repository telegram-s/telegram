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

    public ChatMessage getMessageByMid(int mid) {
        return database.getMessageByMid(mid);
    }

    public ChatMessage[] getMessagesByMid(int[] mids) {
        return database.getMessagesByMid(mids);
    }

    public ChatMessage getMessageById(int id) {
        return database.getMessageById(id);
    }

    public ChatMessage getMessageByRandomId(long rid) {
        return database.getMessageByRid(rid);
    }

    public void create(ChatMessage message) {
        database.create(message);
        application.getDataSourceKernel().onSourceAddMessage(message);
        updateMaxDate(message);
    }

    public void delete(ChatMessage message) {
        database.delete(message);
        application.getDataSourceKernel().onSourceRemoveMessage(message);
        updateMaxDate(message);
    }

    public void update(ChatMessage message) {
        database.update(message);
        application.getDataSourceKernel().onSourceUpdateMessage(message);
        updateMaxDate(message);
    }

    public void deleteHistory(int peerType, int peerId) {
        database.deleteHistory(peerType, peerId);
        application.getDataSourceKernel().removeMessageSource(peerType, peerId);
    }

    public ChatMessage findTopMessage(int peerType, int peerId) {
        return database.findTopMessage(peerType, peerId);
    }

    public ChatMessage[] findDiedMessages(int currentTime) {
        return database.findDiedMessages(currentTime);
    }

    public ChatMessage[] findPendingSelfDestructMessages(int currentTime) {
        return database.findPendingSelfDestructMessages(currentTime);
    }

    public ChatMessage[] findUnreadedSelfDestructMessages(int peerType, int peerId) {
        return database.findUnreadedSelfDestructMessages(peerType, peerId);
    }

    public int getMaxDateInDialog(int peerType, int peerId) {
        return database.getMaxDateInDialog(peerType, peerId);
    }

    public int getMaxMidInDialog(int peerType, int peerId) {
        return database.getMaxMidInDialog(peerType, peerId);
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

    public void onMessageRead(ChatMessage[] messages) {
        for (ChatMessage msg : messages) {
            msg.setState(MessageState.READED);
        }
        database.updateInTx(messages);
    }

    public ChatMessage[] onLoadMoreMessages(List<TLAbsMessage> messages) {
        long start = SystemClock.uptimeMillis();
        ChatMessage[] converted = convert(messages);
        ArrayList<ChatMessage>[] diff = buildDiff(converted);
        Logger.d(TAG, "onLoadMoreMessages:prepare time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        database.diffInTx(diff[0], diff[1]);
        Logger.d(TAG, "onLoadMoreMessages:update time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        for (ChatMessage msg : diff[0]) {
            application.getDataSourceKernel().onSourceUpdateMessage(msg);
        }
        for (ChatMessage msg : diff[1]) {
            application.getDataSourceKernel().onSourceAddMessage(msg);
        }
        Logger.d(TAG, "onLoadMoreMessages:datasource time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        Logger.d(TAG, "onLoadMoreMessages:complete time: " + (SystemClock.uptimeMillis() - start));
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
