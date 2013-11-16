package org.telegram.android.core;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.debug.Assert;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsSentMessage;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLAbsStatedMessages;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.StreamingUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:37
 */
public class ModelEngine {
    private static final String TAG = "ModelEngine";

    private StelsDatabase database;
    private StelsApplication application;

    private ConcurrentHashMap<Integer, User> userCache;

    private PreparedQuery<User> getUserQuery;
    private SelectArg getUserIdArg;

    private int maxDate = 0;
    private AtomicInteger minMid = null;
    private Object minMidSync = new Object();

    public ModelEngine(StelsDatabase database, StelsApplication application) {
        this.database = database;
        this.application = application;
        this.userCache = new ConcurrentHashMap<Integer, User>();

        try {
            QueryBuilder<User, Long> queryBuilder = getUsersDao().queryBuilder();
            getUserIdArg = new SelectArg();
            queryBuilder.where().eq("uid", getUserIdArg);
            getUserQuery = queryBuilder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentTime() {
        return (int) (TimeOverlord.getInstance().getServerTime() / 1000);
    }

    public StelsDatabase getDatabase() {
        return database;
    }

    public RuntimeExceptionDao<MediaRecord, Long> getMediasDao() {
        return database.getMediaDao();
    }

    public RuntimeExceptionDao<DialogDescription, Long> getDialogsDao() {
        return database.getDialogsDao();
    }

    public RuntimeExceptionDao<ChatMessage, Long> getMessagesDao() {
        return database.getMessagesDao();
    }

    public RuntimeExceptionDao<User, Long> getUsersDao() {
        return database.getUsersDao();
    }

    public RuntimeExceptionDao<Contact, Long> getContactsDao() {
        return database.getContactsDao();
    }

    public RuntimeExceptionDao<FullChatInfo, Long> getFullChatInfoDao() {
        return database.getFullChatInfoDao();
    }

    public User getUserRuntime(int id) {
        User res = userCache.get(id);
        if (res != null)
            return res;

        synchronized (getUserQuery) {
            getUserIdArg.setValue(id);
            List<User> users = getUsersDao().query(getUserQuery);
            if (users.size() > 0) {
                userCache.putIfAbsent(id, users.get(0));
                return userCache.get(id);
            }
        }

        throw new RuntimeException("Unknown user #" + id);
    }

    public User getUser(int id) {
        User res = userCache.get(id);
        if (res != null)
            return res;

        synchronized (getUserQuery) {
            getUserIdArg.setValue(id);
            List<User> users = getUsersDao().query(getUserQuery);
            if (users.size() > 0) {
                userCache.putIfAbsent(id, users.get(0));
                return userCache.get(id);
            }
        }

        return null;
    }

    public void updateEncryptedChatKey(TLAbsEncryptedChat chat, byte[] key) {
        int id = chat.getId();
        EncryptedChat encryptedChat = database.getEncryptedChatDao().queryForId((long) id);
        if (encryptedChat != null) {
            encryptedChat.setKey(key);
            database.getEncryptedChatDao().update(encryptedChat);
        }
    }

    public EncryptedChat[] getPendingEncryptedChats() {
        QueryBuilder<EncryptedChat, Long> queryBuilder = database.getEncryptedChatDao().queryBuilder();
        try {
            queryBuilder.where().eq("state", EncryptedChatState.REQUESTED);
            return database.getEncryptedChatDao().query(queryBuilder.prepare()).toArray(new EncryptedChat[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new EncryptedChat[0];
    }

    public void updateEncryptedChat(TLAbsEncryptedChat chat) {
        int id = chat.getId();

        if (id == 0) {
            Logger.w(TAG, "Ignoring encrypted chat");
            return;
        }

        int date = getCurrentTime();

        if (chat instanceof TLEncryptedChat) {
            date = ((TLEncryptedChat) chat).getDate();
        } else if (chat instanceof TLEncryptedChatRequested) {
            date = ((TLEncryptedChatRequested) chat).getDate();
        } else if (chat instanceof TLEncryptedChatWaiting) {
            date = ((TLEncryptedChatWaiting) chat).getDate();
        }

        EncryptedChat encryptedChat = database.getEncryptedChatDao().queryForId((long) id);
        if (encryptedChat != null) {
            if (!(chat instanceof TLEncryptedChat) && !(chat instanceof TLEncryptedChatDiscarded)) {
                return;
            }
            writeEncryptedChatInfo(encryptedChat, chat);
            database.getEncryptedChatDao().update(encryptedChat);
        } else {
            if (!(chat instanceof TLEncryptedChatWaiting) && !(chat instanceof TLEncryptedChatRequested)) {
                return;
            }
            encryptedChat = new EncryptedChat();
            encryptedChat.setId(id);
            writeEncryptedChatInfo(encryptedChat, chat);
            database.getEncryptedChatDao().create(encryptedChat);
        }
        application.getEncryptedChatSource().notifyChatChanged(id);

        DialogDescription description = getDescriptionForPeer(PeerType.PEER_USER_ENCRYPTED, encryptedChat.getId());
//        if (encryptedChat.getState() == EncryptedChatState.DISCARDED) {
//            if (description != null) {
//                description.setDate(0);
//                getDialogsDao().update(description);
//                application.getDialogSource().getViewSource().removeItem(description);
//            }
//        } else {
        if (description == null) {
            description = createDescriptionForEncryptedUser(encryptedChat.getId(), encryptedChat.getUserId());
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            getDialogsDao().create(description);
            application.getDialogSource().getViewSource().addItem(description);

            if (encryptedChat.getState() == EncryptedChatState.REQUESTED) {
                User user = application.getEngine().getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatRequested(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            }
        } else {
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.DISCARDED:
                    description.setMessage("Chat discarded");
                    description.setExtras(new TLLocalActionEncryptedCancelled());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);

            if (encryptedChat.getState() == EncryptedChatState.NORMAL) {
                User user = application.getEngine().getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatEstablished(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            } else if (encryptedChat.getState() == EncryptedChatState.DISCARDED) {
                User user = application.getEngine().getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatCancelled(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            }
        }
        //}
    }

    private void writeEncryptedChatInfo(EncryptedChat chat, TLAbsEncryptedChat rawChat) {
        if (rawChat instanceof TLEncryptedChatRequested) {
            TLEncryptedChatRequested requested = (TLEncryptedChatRequested) rawChat;
            chat.setAccessHash(requested.getAccessHash());
            chat.setUserId(requested.getAdminId());
            byte[] tmpKey = CryptoUtils.concat(
                    StreamingUtils.intToBytes(requested.getGA().length),
                    requested.getGA(),
                    StreamingUtils.intToBytes(requested.getNonce().length),
                    requested.getNonce());
            chat.setKey(tmpKey);
            chat.setState(EncryptedChatState.REQUESTED);
            chat.setOut(false);
        } else if (rawChat instanceof TLEncryptedChatWaiting) {
            TLEncryptedChatWaiting waiting = (TLEncryptedChatWaiting) rawChat;
            chat.setAccessHash(waiting.getAccessHash());
            chat.setUserId(waiting.getParticipantId());
            chat.setState(EncryptedChatState.WAITING);
            chat.setOut(true);
        } else if (rawChat instanceof TLEncryptedChatDiscarded) {
            chat.setState(EncryptedChatState.DISCARDED);
        } else if (rawChat instanceof TLEncryptedChat) {
            chat.setState(EncryptedChatState.NORMAL);
        }
    }

    public void deleteEncryptedChat(int chatId) {
        database.getEncryptedChatDao().deleteById((long) chatId);
    }

    public EncryptedChat getEncryptedChat(int chatId) {
        return database.getEncryptedChatDao().queryForId((long) chatId);
    }

    public void setSelfDestructTimer(int chatId, int time) {
        EncryptedChat chat = getEncryptedChat(chatId);
        chat.setSelfDestructTime(time);
        database.getEncryptedChatDao().update(chat);
    }

    public void markDialogAsNonFailed(int peerType, int peerId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setFailure(false);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void markUnsentAsFailured() {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = database.getMessagesDao().queryBuilder();
            queryBuilder.where().eq("state", MessageState.PENDING).and().eq("isOut", true).and().eq("deletedLocal", false);
            for (ChatMessage message : database.getMessagesDao().query(queryBuilder.prepare())) {
                onMessageFailure(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ChatMessage[] getUnsyncedDeletedMessages() {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = database.getMessagesDao().queryBuilder();
            queryBuilder.where().eq("deletedLocal", true).and().eq("deletedServer", false);
            return database.getMessagesDao().query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ChatMessage[0];
    }

    public ChatMessage[] getUnsyncedRestoredMessages() {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = database.getMessagesDao().queryBuilder();
            queryBuilder.where().eq("deletedLocal", false).and().eq("deletedServer", true);
            return database.getMessagesDao().query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ChatMessage[0];
    }

    private ChatMessage[] getMessagesById(Object[] mid) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where().in("mid", mid);
            return queryBuilder.query().toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new ChatMessage[0];
    }

    public ChatMessage getMessageByDbId(int localId) {
        return getMessagesDao().queryForId((long) localId);
    }

    public int getMaxMsgInId(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> builder = getMessagesDao().queryBuilder();
            builder.where().eq("peerType", peerType).and().eq("peerId", peerId).and().eq("isOut", false);
            builder.orderBy("mid", false);
            ChatMessage message = getMessagesDao().queryForFirst(builder.prepare());
            if (message != null) {
                return message.getMid();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getMaxDateInDialog(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> builder = getMessagesDao().queryBuilder();
            builder.where().eq("peerType", peerType).and().eq("peerId", peerId).and().eq("isOut", false);
            builder.orderBy("date", false);
            ChatMessage message = getMessagesDao().queryForFirst(builder.prepare());
            if (message != null) {
                return message.getDate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public ChatMessage getMessageById(int mid) {
        List<ChatMessage> res = getMessagesDao().queryForEq("mid", mid);
        if (res.size() == 0) {
            return null;
        } else {
            return res.get(0);
        }
    }

    /**
     * Doesn't save order
     *
     * @param mid
     * @return
     */
    private User[] getUsersByIdRaw(Object[] mid) {
        return getUsersById(mid);
//        try {
//            ArrayList<User> users = new ArrayList<User>();
//            QueryBuilder<User, Long> queryBuilder = getUsersDao().queryBuilder();
//            queryBuilder.where().in("uid", mid);
//            User[] res = queryBuilder.query().toArray(new User[0]);
//            for (int i = 0; i < res.length; i++) {
//                users.add(cacheUser(res[i]));
//            }
//            return res;
//        } catch (SQLException e) {
//            Logger.t(TAG, e);
//        }
//
//        return new User[0];
    }

    /**
     * Doesn't save order
     *
     * @param mid
     * @return
     */
    public User[] getUsersById(Object[] mid) {
        try {
            ArrayList<Object> missedMids = new ArrayList<Object>();
            ArrayList<User> users = new ArrayList<User>();
            for (Object o : mid) {
                if (userCache.containsKey(o)) {
                    users.add(userCache.get(o));
                } else {
                    missedMids.add(o);
                }
            }
            if (missedMids.size() > 0) {
                QueryBuilder<User, Long> queryBuilder = getUsersDao().queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                User[] res = queryBuilder.query().toArray(new User[0]);
                for (int i = 0; i < res.length; i++) {
                    users.add(cacheUser(res[i]));
                }
            }
            return users.toArray(new User[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new User[0];
    }

    public void cacheUsers(Object[] mid) {
        try {
            ArrayList<Object> missedMids = new ArrayList<Object>();
            ArrayList<User> users = new ArrayList<User>();
            for (Object o : mid) {
                if (userCache.containsKey(o)) {
                    users.add(userCache.get(o));
                } else {
                    missedMids.add(o);
                }
            }
            if (missedMids.size() > 0) {
                QueryBuilder<User, Long> queryBuilder = getUsersDao().queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                User[] res = queryBuilder.query().toArray(new User[0]);
                for (int i = 0; i < res.length; i++) {
                    users.add(cacheUser(res[i]));
                }
            }
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

    }

    public User cacheUser(User src) {
        userCache.putIfAbsent(src.getUid(), src);
        return userCache.get(src.getUid());
    }

    public FullChatInfo getFullChatInfo(int chatId) {
        return getFullChatInfoDao().queryForId((long) chatId);
    }

    public DialogDescription getDescriptionForPeer(int peerType, int peerId) {
        try {
            QueryBuilder<DialogDescription, Long> builder = getDialogsDao().queryBuilder();
            builder.where().eq("peerType", peerType).and().eq("peerId", peerId).and();
            return getDialogsDao().queryForFirst(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<DialogDescription> getUnreadedRemotelyDescriptions() {
        try {
            QueryBuilder<DialogDescription, Long> builder = getDialogsDao().queryBuilder();
            builder.where().raw("lastLocalViewedMessage > lastRemoteViewedMessage");
            return getDialogsDao().query(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArrayList<DialogDescription>();
    }

    public void onNewUnreadMessageId(int peerType, int peerId, int mid) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            if (description.getLastLocalViewedMessage() < mid) {
                if (description.getFirstUnreadMessage() == 0 || (mid < description.getFirstUnreadMessage())) {
                    description.setFirstUnreadMessage(mid);
                    Logger.d(TAG, "Setting first unread message: " + mid + " (" + peerType + ":" + peerId + ")");
                }
                description.setUnreadCount(description.getUnreadCount() + 1);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
    }

    public void onNewUnreadEncMessage(int chatId, int date) {
        DialogDescription description = getDescriptionForPeer(PeerType.PEER_USER_ENCRYPTED, chatId);
        if (description != null) {
            if (description.getLastLocalViewedMessage() < date) {
                description.setUnreadCount(description.getUnreadCount() + 1);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
    }

    public void onMaxLocalViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setLastLocalViewedMessage(maxId);
            description.setUnreadCount(0);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void clearFirstUnreadMessage(int peerType, int peerId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setFirstUnreadMessage(0);
            description.setUnreadCount(0);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void onMaxRemoteViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setLastRemoteViewedMessage(maxId);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void onChatTitleChanges(int chatId, String title) {
        DialogDescription description = getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (description != null) {
            description.setTitle(title);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public ChatMessage[] findDiedMessages(int currentTime) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where().le("messageDieTime", currentTime).and().ne("messageDieTime", 0);
            return getMessagesDao().query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public ChatMessage[] findPendingSelfDestructMessages(int currentTime) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where().gt("messageDieTime", currentTime).and().ne("messageDieTime", 0);
            return getMessagesDao().query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public ChatMessage[] findUnreadedSelfDestructMessages(int peerType, int peerId) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where().ne("messageTimeout", 0).and().eq("messageDieTime", 0).and().eq("peerType", peerType).and().eq("peerId", peerId);
            return getMessagesDao().query(queryBuilder.prepare()).toArray(new ChatMessage[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new ChatMessage[0];
    }

    public void onChatAvatarChanges(int chatId, TLAbsPhoto photo) {
        DialogDescription description = getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (description != null) {
            description.setPhoto(EngineUtils.convertAvatarPhoto(photo));
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void onChatUserAdded(int chatId, int inviter, int uid) {
        FullChatInfo chatInfo = getFullChatInfo(chatId);
        if (chatInfo != null) {
            if (chatInfo.isForbidden()) {
                if (uid == application.getCurrentUid()) {
                    chatInfo.setForbidden(false);
                    chatInfo.setUids(new int[]{uid});
                    chatInfo.setInviters(new int[]{inviter});
                    getFullChatInfoDao().update(chatInfo);
                    application.getChatSource().notifyChatChanged(chatId);
                }
            } else {

                ArrayList<Pair<Integer, Integer>> uids = new ArrayList<Pair<Integer, Integer>>();
                for (int i = 0; i < chatInfo.getUids().length; i++) {
                    if (chatInfo.getUids()[i] != uid) {
                        uids.add(new Pair<Integer, Integer>(
                                chatInfo.getUids()[i],
                                chatInfo.getInviters()[i]));
                    }
                }

                uids.add(new Pair<Integer, Integer>(uid, inviter));

                int[] newUids = new int[uids.size()];
                int[] newInviters = new int[uids.size()];

                int index = 0;
                for (Pair<Integer, Integer> u : uids) {
                    newUids[index] = u.first;
                    newInviters[index] = u.second;
                    index++;
                }

                chatInfo.setUids(newUids);
                chatInfo.setInviters(newInviters);
                getFullChatInfoDao().update(chatInfo);
                application.getChatSource().notifyChatChanged(chatId);
            }
        }

        DialogDescription description = getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (description != null) {
            description.setParticipantsCount(description.getParticipantsCount() + 1);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public void onChatUserRemoved(int chatId, int uid) {
        FullChatInfo chatInfo = getFullChatInfo(chatId);
        if (chatInfo != null) {
            if (uid == application.getCurrentUid()) {
                chatInfo.setForbidden(true);
                chatInfo.setInviters(null);
                chatInfo.setUids(null);
                getFullChatInfoDao().update(chatInfo);
                application.getChatSource().notifyChatChanged(chatId);
            } else {
                ArrayList<Pair<Integer, Integer>> uids = new ArrayList<Pair<Integer, Integer>>();
                for (int i = 0; i < chatInfo.getUids().length; i++) {
                    if (chatInfo.getUids()[i] != uid) {
                        uids.add(new Pair<Integer, Integer>(
                                chatInfo.getUids()[i],
                                chatInfo.getInviters()[i]));
                    }
                }

                int[] newUids = new int[uids.size()];
                int[] newInviters = new int[uids.size()];

                int index = 0;
                for (Pair<Integer, Integer> u : uids) {
                    newUids[index] = u.first;
                    newInviters[index] = u.second;
                    index++;
                }

                chatInfo.setUids(newUids);
                chatInfo.setInviters(newInviters);
                getFullChatInfoDao().update(chatInfo);
                application.getChatSource().notifyChatChanged(chatId);
            }
        }

        DialogDescription description = getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (description != null) {
            if (uid == application.getCurrentUid()) {
                description.setParticipantsCount(0);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            } else {
                description.setParticipantsCount(description.getParticipantsCount() - 1);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
    }

    public void onChatParticipants(TLAbsChatParticipants participants) {
        int chatId = participants.getChatId();
        FullChatInfo info = getFullChatInfo(chatId);
        if (info != null) {
            if (participants instanceof TLChatParticipants) {
                TLChatParticipants tlChatParticipants = (TLChatParticipants) participants;
                if (info.getVersion() > tlChatParticipants.getVersion()) {
                    return;
                }
                info.setVersion(tlChatParticipants.getVersion());
                info.setAdminId(tlChatParticipants.getAdminId());
                info.setForbidden(false);
                int[] uids = new int[tlChatParticipants.getParticipants().size()];
                int[] inviters = new int[uids.length];
                int index = 0;
                for (TLChatParticipant participant : tlChatParticipants.getParticipants()) {
                    uids[index] = participant.getUserId();
                    inviters[index] = participant.getInviterId();
                    index++;
                }
                info.setInviters(inviters);
                info.setUids(uids);
                getFullChatInfoDao().update(info);
                application.getChatSource().notifyChatChanged(chatId);
            } else {
                info.setForbidden(true);
                getFullChatInfoDao().update(info);
                application.getChatSource().notifyChatChanged(chatId);
            }
        } else {
            info = new FullChatInfo();
            if (participants instanceof TLChatParticipants) {
                TLChatParticipants tlChatParticipants = (TLChatParticipants) participants;
                if (info.getVersion() > tlChatParticipants.getVersion()) {
                    return;
                }
                info.setChatId(chatId);
                info.setVersion(tlChatParticipants.getVersion());
                info.setAdminId(tlChatParticipants.getAdminId());
                info.setForbidden(false);
                int[] uids = new int[tlChatParticipants.getParticipants().size()];
                int[] inviters = new int[uids.length];
                int index = 0;
                for (TLChatParticipant participant : tlChatParticipants.getParticipants()) {
                    uids[index] = participant.getUserId();
                    inviters[index] = participant.getInviterId();
                    index++;
                }
                info.setInviters(inviters);
                info.setUids(uids);
                getFullChatInfoDao().create(info);
                application.getChatSource().notifyChatChanged(chatId);
            } else {
                info.setChatId(chatId);
                info.setForbidden(true);
                getFullChatInfoDao().create(info);
                application.getChatSource().notifyChatChanged(chatId);
            }
        }

        DialogDescription description = getDescriptionForPeer(PeerType.PEER_CHAT, chatId);
        if (description != null) {
            if (info.isForbidden()) {
                description.setParticipantsCount(0);
            } else {
                description.setParticipantsCount(info.getUids().length);
            }
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        }
    }

    public boolean onUserAvatarChanges(int uid, TLAbsLocalAvatarPhoto photo) {
        User u = getUser(uid);
        if (u != null) {
            u.setPhoto(photo);
            getUsersDao().update(u);

            DialogDescription description = getDescriptionForPeer(PeerType.PEER_USER, uid);
            if (description != null) {
                description.setPhoto(photo);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                return true;
            }
        }
        return false;
    }

    public boolean onUserNameChanges(int uid, String firstName, String lastName) {
        User u = getUser(uid);
        if (u != null) {
            u.setFirstName(firstName);
            u.setLastName(lastName);
            getUsersDao().update(u);

            DialogDescription description = getDescriptionForPeer(PeerType.PEER_USER, uid);
            if (description != null) {
                description.setTitle(firstName + " " + lastName);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                return true;
            }
        }
        return false;
    }

    public void onUserStatus(int uid, TLAbsUserStatus status) {
        User u = getUser(uid);
        if (u != null) {
            u.setStatus(EngineUtils.convertStatus(status));
            getUsersDao().update(u);
        }
    }

    public void onImportedContacts(final Map<Integer, Long> importedContacts) {
        getContactsDao().callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (Integer uid : importedContacts.keySet()) {
                    Contact contact = getUidContact(uid);
                    if (contact != null) {
                        contact.setLocalId(importedContacts.get(uid));
                        getContactsDao().update(contact);
                    }
                }
                return null;
            }
        });
    }

    public Contact getUidContact(int uid) {
        try {
            QueryBuilder<Contact, Long> builder = getContactsDao().queryBuilder();
            builder.where().eq("uid", uid);
            return getContactsDao().queryForFirst(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Contact getContactUid(int localId) {
        try {
            QueryBuilder<Contact, Long> builder = getContactsDao().queryBuilder();
            builder.where().eq("localId", localId);
            return getContactsDao().queryForFirst(builder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateContact(int uid, long localId) {
        Contact contact = getUidContact(uid);
        contact.setLocalId(localId);
        getContactsDao().update(contact);
    }

    public void addContact(int uid, long localId) {
        if (getUidContact(uid) != null)
            return;
        User user = getUser(uid);
        if (user == null) {
            return;
        }
        getContactsDao().create(new Contact(uid, localId, user, false));
    }

    public void onContacts(List<TLAbsUser> users, List<TLContact> contacts) {
        onUsers(users);

        final List<Contact> converted = new ArrayList<Contact>();
        for (TLContact contact : contacts) {
            converted.add(new Contact(contact.getUserId(), 0, getUser(contact.getUserId()), contact.getMutual()));
        }

        DiffUtils.applyDiffUpdate(converted, getContactsDao(), new Comparator<Contact>() {
            @Override
            public int compare(Contact contact, Contact contact2) {
                if (contact.getUid() == contact2.getUid()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });
    }

    public void onUserLinkChanged(int uid, int link) {
        User user = getUser(uid);
        if (user == null) {
            return;
        }
        user.setLinkType(link);
        getUsersDao().update(user);
    }

    public void onUsers(List<TLAbsUser> users) {
        long start = SystemClock.uptimeMillis();
        Integer[] ids = new Integer[users.size()];
        User[] converted = new User[users.size()];
        for (int i = 0; i < ids.length; i++) {
            converted[i] = EngineUtils.userFromTlUser(users.get(i));
            ids[i] = converted[i].getUid();
        }

        User[] original = getUsersByIdRaw(ids);

        final ArrayList<Pair<User, User>> diff = new ArrayList<Pair<User, User>>();

        for (User m : converted) {
            User orig = EngineUtils.searchUser(original, m.getUid());
            diff.add(new Pair<User, User>(orig, m));
        }

        Logger.d(TAG, "onUsers:prepare: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        getUsersDao().callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (Pair<User, User> user : diff) {
                    User orig = user.first;
                    User changed = user.second;

                    if (orig == null) {
                        getUsersDao().create(changed);
                        userCache.putIfAbsent(changed.getUid(), changed);
                        if (changed.getUid() == application.getCurrentUid()) {
                            if (application.getUserSource() != null) {
                                application.getUserSource().notifyUserChanged(application.getCurrentUid());
                            }
                        }
                    } else {
                        orig.setFirstName(changed.getFirstName());
                        orig.setLastName(changed.getLastName());
                        orig.setPhone(changed.getPhone());
                        orig.setStatus(changed.getStatus());
                        orig.setAccessHash(changed.getAccessHash());
                        orig.setLinkType(changed.getLinkType());
                        getUsersDao().update(orig);
                    }
                }
                return null;
            }
        });

        if (Assert.ENABLED) {
            for (Integer id : ids) {
                synchronized (getUserQuery) {
                    getUserIdArg.setValue(id);
                    List<User> users2 = getUsersDao().query(getUserQuery);
                    if (users2.size() == 0) {
                        throw new AssertionError("User #" + id + " not saved to DB!");
                    }
                }
            }
        }

        Logger.d(TAG, "onUsers:update: " + (SystemClock.uptimeMillis() - start));
    }

    private void updateMaxDate(ChatMessage msg) {
        if (msg.getDate() > maxDate) {
            maxDate = msg.getDate();
        }
    }

    private int getMaxDate() {
        if (maxDate == 0) {
            try {
                ChatMessage msg = getMessagesDao().queryForFirst(getMessagesDao().queryBuilder().orderBy("date", false).prepare());
                if (msg != null) {
                    maxDate = msg.getDate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return maxDate;
    }

    private int getMinMid() {
        if (minMid == null) {
            synchronized (minMidSync) {
                try {
                    ChatMessage msg = getMessagesDao().queryForFirst(getMessagesDao().queryBuilder().orderBy("mid", true).prepare());
                    if (msg == null) {
                        minMid = new AtomicInteger(0);
                    } else {
                        minMid = new AtomicInteger(Math.min(msg.getMid(), 0));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return minMid.getAndDecrement();
    }

    public void onEncryptedReaded(int chatId, int readDate, int maxDate) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where()
                    .eq("peerType", PeerType.PEER_USER_ENCRYPTED).and()
                    .eq("peerId", chatId).and()
                    .eq("isOut", true).and()
                    .eq("state", MessageState.SENT).and()
                    .le("date", maxDate);

            int localReadDate = (int) (readDate - TimeOverlord.getInstance().getTimeDelta() / 1000);
            for (ChatMessage msg : getMessagesDao().query(queryBuilder.prepare())) {
                if (msg.getMessageTimeout() > 0) {
                    msg.setMessageDieTime(localReadDate + msg.getMessageTimeout());
                    application.getSelfDestructProcessor().performSelfDestruct(msg.getDatabaseId(), msg.getMessageDieTime());
                }
                markReaded(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onMessagesReaded(Integer[] mids) {
        ChatMessage[] saved = getMessagesById(mids);
        for (int mid : mids) {
            ChatMessage msg = EngineUtils.searchMessage(saved, mid);
            if (msg != null) {
                markReaded(msg);
            } else {
                Logger.w(TAG, "Mark unknown");
            }
        }
    }

    private void markReaded(ChatMessage msg) {
        if (!msg.isOut() && msg.getState() == MessageState.SENT) {
            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null) {
                description.setUnreadCount(Math.max(0, description.getUnreadCount() - 1));
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
        if (msg.isOut()) {
            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null && description.getTopMessageId() == msg.getMid()) {
                description.setMessageState(MessageState.READED);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
        if (msg.getState() != MessageState.READED) {
            Logger.w(TAG, "Mark readed: " + msg.getMid());
        } else {
            Logger.w(TAG, "Already readed: " + msg.getMid());
        }
        msg.setState(MessageState.READED);
        getMessagesDao().update(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
    }

    public boolean onNewShortMessage(int peerType, int peerId, int mid, int date, int senderId, String message) {
        List<ChatMessage> msgs = getMessagesDao().queryForEq("mid", mid);
        if (msgs.size() != 0) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(mid);
        nmsg.setDate(date);
        nmsg.setMessage(message);
        nmsg.setContentType(ContentType.MESSAGE_TEXT);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
        return true;
    }

    public boolean onNewShortEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, String message) {
        List<ChatMessage> msgs = getMessagesDao().queryForEq("randomId", randomId);
        if (msgs.size() != 0) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(getMinMid() - 1);
        nmsg.setDate(date);
        nmsg.setMessage(message);
        nmsg.setContentType(ContentType.MESSAGE_TEXT);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
        return true;
    }

    public boolean onNewLocationEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, double longitude, double latitude) {
        List<ChatMessage> msgs = getMessagesDao().queryForEq("randomId", randomId);
        if (msgs.size() != 0) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(getMinMid() - 1);
        nmsg.setDate(date);
        nmsg.setMessage("Geo");
        nmsg.setContentType(ContentType.MESSAGE_GEO);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(new TLLocalGeo(latitude, longitude));
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
        return true;
    }

    public boolean onNewPhotoEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLLocalPhoto photo) {
        List<ChatMessage> msgs = getMessagesDao().queryForEq("randomId", randomId);
        if (msgs.size() != 0) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(getMinMid() - 1);
        nmsg.setDate(date);
        nmsg.setMessage("Photo");
        nmsg.setContentType(ContentType.MESSAGE_PHOTO);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(photo);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        saveMedia(nmsg.getMid(), nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
        return true;
    }

    public boolean onNewVideoEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLLocalVideo video) {
        List<ChatMessage> msgs = getMessagesDao().queryForEq("randomId", randomId);
        if (msgs.size() != 0) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(getMinMid() - 1);
        nmsg.setDate(date);
        nmsg.setMessage("Video");
        nmsg.setContentType(ContentType.MESSAGE_VIDEO);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(video);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        saveMedia(nmsg.getMid(), nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
        return true;
    }

    public void onNewInternalServiceMessage(int peerType, int peerId, int senderId, int date, TLAbsLocalAction action) {
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(getMinMid() - 1);
        nmsg.setDate(date);
        nmsg.setMessage("");
        nmsg.setContentType(ContentType.MESSAGE_SYSTEM);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(action);
        nmsg.setOut(senderId == application.getCurrentUid());
        getMessagesDao().create(nmsg);
        application.getDataSourceKernel().onSourceAddMessage(nmsg);
        updateDescriptorShort(nmsg);
        updateMaxDate(nmsg);
    }

    public int sendVideo(int peerType, int peerId, String fileName, int previewW, int previewH) {
        TLUploadingVideo video = new TLUploadingVideo();
        video.setFileName(fileName);
        video.setPreviewWidth(previewW);
        video.setPreviewHeight(previewH);
        return sendVideo(peerType, peerId, video);
    }

    public int sendPhotoUri(int peerType, int peerId, String uri, int width, int height) {
        TLUploadingPhoto photo = new TLUploadingPhoto(width, height);
        photo.setFileUri(uri);
        photo.setFileName("");
        return sendPhoto(peerType, peerId, photo);
    }

    public int sendPhoto(int peerType, int peerId, String fileName, int width, int height) {
        TLUploadingPhoto photo = new TLUploadingPhoto(width, height);
        photo.setFileName(fileName);
        photo.setFileUri("");
        return sendPhoto(peerType, peerId, photo);
    }

    private ChatMessage prepareSendMessage(int peerType, int peerId) {
        ChatMessage msg = new ChatMessage();
        msg.setMid(getMinMid() - 1);
        msg.setPeerId(peerId);
        msg.setPeerType(peerType);
        msg.setOut(true);
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setSenderId(application.getCurrentUid());
        msg.setState(MessageState.PENDING);
        msg.setRandomId(EngineUtils.generateRandomId());
        updateMaxDate(msg);
        return msg;
    }

    private ChatMessage prepareSendMessageAsync(int peerType, int peerId) {
        ChatMessage msg = new ChatMessage();
        msg.setPeerId(peerId);
        msg.setPeerType(peerType);
        msg.setOut(true);
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setSenderId(application.getCurrentUid());
        msg.setState(MessageState.PENDING);
        msg.setRandomId(EngineUtils.generateRandomId());
        updateMaxDate(msg);
        return msg;
    }

    private int sendPhoto(int peerType, int peerId, TLUploadingPhoto photo) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Photo");
        msg.setContentType(ContentType.MESSAGE_PHOTO);
        msg.setExtras(photo);
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMedia(msg);
        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    private int sendVideo(int peerType, int peerId, TLUploadingVideo video) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Video");
        msg.setContentType(ContentType.MESSAGE_VIDEO);
        msg.setExtras(video);
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMedia(msg);
        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int forwardMessage(int peerType, int peerId, int mid) {
        ChatMessage src = getMessageById(mid);

        if (src == null)
            return -1;

        ChatMessage msg = prepareSendMessage(peerType, peerId);

        msg.setForwardMid(src.getMid());
        msg.setMessage(src.getMessage());
        if (src.getRawContentType() == ContentType.MESSAGE_PHOTO) {
            msg.setContentType(ContentType.MESSAGE_PHOTO | ContentType.MESSAGE_FORWARDED);
        } else if (src.getRawContentType() == ContentType.MESSAGE_VIDEO) {
            msg.setContentType(ContentType.MESSAGE_VIDEO | ContentType.MESSAGE_FORWARDED);
        } else if (src.getRawContentType() == ContentType.MESSAGE_CONTACT) {
            msg.setContentType(ContentType.MESSAGE_CONTACT | ContentType.MESSAGE_FORWARDED);
        } else if (src.getRawContentType() == ContentType.MESSAGE_GEO) {
            msg.setContentType(ContentType.MESSAGE_GEO | ContentType.MESSAGE_FORWARDED);
        } else {
            msg.setContentType(ContentType.MESSAGE_TEXT | ContentType.MESSAGE_FORWARDED);
        }

        if (src.isForwarded()) {
            msg.setForwardDate(src.getForwardDate());
            msg.setForwardSenderId(src.getForwardSenderId());
        } else {
            msg.setForwardDate(src.getDate());
            msg.setForwardSenderId(src.getSenderId());
        }

        msg.setExtras(src.getExtras());
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMessage(msg);

        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int shareContact(int peerType, int peerId, int uid) {
        User src = getUser(uid);

        if (src == null)
            return -1;

        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setContentType(ContentType.MESSAGE_CONTACT);
        msg.setMessage("");
        msg.setExtras(new TLLocalContact(src.getPhone(), src.getFirstName(), src.getLastName(), src.getUid()));

        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMessage(msg);

        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int sendLocation(int peerType, int peerId, TLLocalGeo point) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("");
        msg.setContentType(ContentType.MESSAGE_GEO);
        msg.setExtras(point);
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMessage(msg);
        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public ChatMessage prepareAsyncSendMessage(int peerType, int peerId, String message) {
        ChatMessage msg = prepareSendMessageAsync(peerType, peerId);
        msg.setMessage(message);
        msg.setContentType(ContentType.MESSAGE_TEXT);
        msg.setDatabaseId(Entropy.randomInt());
        application.getDataSourceKernel().onSourceAddMessageHacky(msg);
        return msg;
    }

    public void offThreadSendMessage(ChatMessage msg) {
        msg.setMid(getMinMid() - 1);
        application.getDataSourceKernel().onSourceRemoveMessage(msg);
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        updateDescriptorPending(msg);
    }

    public int sendMessage(int peerType, int peerId, String message) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage(message);
        msg.setContentType(ContentType.MESSAGE_TEXT);
        getMessagesDao().create(msg);
        application.getDataSourceKernel().onSourceAddMessage(msg);
        application.getMessageSender().sendMessage(msg);
        updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public void tryAgainMedia(int localId) {
        ChatMessage msg = getMessageByDbId(localId);
        msg.setMid(getMinMid() - 1);
        int date = (int) (System.currentTimeMillis() / 1000);
        int maxDate = getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setState(MessageState.PENDING);
        msg.setRandomId(EngineUtils.generateRandomId());
        getMessagesDao().update(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        application.getMessageSender().sendMedia(msg);
        updateDescriptorPending(msg);
    }

    public void tryAgain(int localId) {
        ChatMessage msg = getMessageByDbId(localId);
        msg.setMid(getMinMid() - 1);
        int date = (int) (System.currentTimeMillis() / 1000);
        int maxDate = getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setState(MessageState.PENDING);
        msg.setRandomId(EngineUtils.generateRandomId());
        getMessagesDao().update(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        application.getMessageSender().sendMessage(msg);
        updateDescriptorPending(msg);
    }

    private DialogDescription createDescriptionForUser(int uid) {
        DialogDescription res = new DialogDescription();
        res.setPeerType(PeerType.PEER_USER);
        res.setPeerId(uid);
        User user = getUser(uid);
        res.setTitle(user.getDisplayName());
        res.setPhoto(user.getPhoto());
        return res;
    }

    private DialogDescription createDescriptionForEncryptedUser(int chatId, int uid) {
        DialogDescription res = new DialogDescription();
        res.setPeerType(PeerType.PEER_USER_ENCRYPTED);
        res.setPeerId(chatId);
        User user = getUserRuntime(uid);
        res.setTitle(user.getDisplayName());
        res.setPhoto(user.getPhoto());
        return res;
    }

    private DialogDescription createDescriptionForChat(int chatId, TLAbsChat chat, TLDialog dialog) {
        DialogDescription res = new DialogDescription();
        res.setPeerType(PeerType.PEER_CHAT);
        res.setPeerId(chatId);
        if (chat instanceof TLChat) {
            res.setTitle(((TLChat) chat).getTitle());
            res.setPhoto(EngineUtils.convertAvatarPhoto(((TLChat) chat).getPhoto()));
            res.setParticipantsCount(((TLChat) chat).getParticipantsCount());
        } else if (chat instanceof TLChatForbidden) {
            res.setTitle(((TLChatForbidden) chat).getTitle());
            res.setPhoto(null);
            res.setParticipantsCount(0);
        } else /*if (chat instanceof TLChatEmpty)*/ {
            res.setTitle("Unknown chat");
            res.setPhoto(null);
            res.setParticipantsCount(0);
        }
        return res;
    }

    private void applyDescriptor(DialogDescription description, ChatMessage msg) {
        if (msg.getState() == MessageState.PENDING || msg.getState() == MessageState.FAILURE) {
            description.setTopMessageId(-msg.getDatabaseId());
        } else {
            description.setTopMessageId(msg.getMid());
        }
        description.setMessage(msg.getMessage());
        description.setDate(msg.getDate());
        description.setContentType(msg.getContentType());
        if (msg.isOut()) {
            description.setMessageState(msg.getState());
        } else {
            description.setMessageState(-1);
        }
        description.setSenderId(msg.getSenderId());
        description.setExtras(msg.getExtras());
        User senderUser = getUser(msg.getSenderId());
        description.setSenderTitle(senderUser.getDisplayName());
    }

    private void updateDescriptorPending(ChatMessage msg) {
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            applyDescriptor(description, msg);
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
        } else {
            if (msg.getPeerType() == PeerType.PEER_USER) {
                description = createDescriptionForUser(msg.getPeerId());
                applyDescriptor(description, msg);
                getDialogsDao().create(description);
                application.getDialogSource().getViewSource().addItem(description);
            }
        }
    }

    private void updateDescriptorShort(ChatMessage msg) {
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getDate() <= msg.getDate()) {
                applyDescriptor(description, msg);
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        } else {
            if (msg.getPeerType() == PeerType.PEER_USER) {
                description = createDescriptionForUser(msg.getPeerId());
                applyDescriptor(description, msg);
                getDialogsDao().create(description);
                application.getDialogSource().getViewSource().addItem(description);
            }
        }
    }

    private void updateDescriptorDeleteUnsent(int peerType, int peerId, int localId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -localId) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    private void updateDescriptorDeleteSent(int peerType, int peerId, int mid) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == mid) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    private void updateDescriptorSelfDestructed(int peerType, int peerId, int mid) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == mid) {
                description.setMessageState(MessageState.READED);
                description.setContentType(ContentType.MESSAGE_SYSTEM);
                description.setMessage("");
                description.setExtras(new TLLocalActionEncryptedMessageDestructed());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
    }

    private void updateDescriptorFromScratch(int peerType, int peerId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            try {
                QueryBuilder<ChatMessage, Long> builder = getMessagesDao().queryBuilder();
                builder.where().eq("deletedLocal", false).and().eq("peerType", peerType).and().eq("peerId", peerId);
                builder.orderBy("date", false);
                ChatMessage msg = getMessagesDao().queryForFirst(builder.prepare());
                if (msg == null) {
                    description.setDate(0);
                    description.setTopMessageId(0);
                    description.setMessage("");
                    description.setContentType(0);
                    description.setPhoto(null);
                    description.setParticipantsCount(0);
                    description.setSenderId(0);
                    description.setSenderTitle(null);
                    getDialogsDao().update(description);
                    application.getDialogSource().getViewSource().removeItem(description);
                } else {
                    applyDescriptor(description, msg);
                    getDialogsDao().update(description);
                    application.getDialogSource().getViewSource().updateItem(description);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void onConfirmed(ChatMessage msg) {
        if (msg.getState() == MessageState.PENDING) {
            msg.setState(MessageState.SENT);
            application.getDataSourceKernel().onSourceUpdateMessage(msg);
            getMessagesDao().update(msg);

            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null) {
                if (description.getTopMessageId() == -msg.getDatabaseId()) {
                    description.setMessageState(MessageState.SENT);
                    description.setDate(msg.getDate());
                    getDialogsDao().update(description);
                    application.getDialogSource().getViewSource().updateItem(description);
                }
            }
        }
    }

    public synchronized void onForwarded(ChatMessage msg, TLAbsStatedMessages statedMessages) {
        int mid = statedMessages.getMessages().get(0).getId();

        List<ChatMessage> msgs = getMessagesDao().queryForEq("mid", mid);
        if (msgs.size() != 0) {
            getMessagesDao().delete(msg);
            application.getDataSourceKernel().onSourceRemoveMessage(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);

//        if (msg.getRawContentType() == ContentType.MESSAGE_PHOTO || msg.getRawContentType() == ContentType.MESSAGE_VIDEO) {
//            saveMedia(mid, msg);
//        }

        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        getMessagesDao().update(msg);
        onNewMessages(statedMessages.getMessages(), statedMessages.getUsers(),
                statedMessages.getChats(), new ArrayList<TLDialog>());
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
            }
        }
    }

    public synchronized void onMessageSent(ChatMessage msg, TLAbsStatedMessage statedMessage) {
        int mid = ((TLMessage) statedMessage.getMessage()).getId();
        List<ChatMessage> msgs = getMessagesDao().queryForEq("mid", mid);
        if (msgs.size() != 0) {
            getMessagesDao().delete(msg);
            application.getDataSourceKernel().onSourceRemoveMessage(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        updateMaxDate(msg);
        getMessagesDao().update(msg);
        List<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
        messages.add(statedMessage.getMessage());
        onNewMessages(messages, statedMessage.getUsers(), statedMessage.getChats(), new ArrayList<TLDialog>());

        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                application.getDialogSource().getViewSource().invalidateData();
            }
        }
    }

    public synchronized void onMessageSent(ChatMessage msg, TLAbsSentMessage tl) {

        List<ChatMessage> msgs = getMessagesDao().queryForEq("mid", tl.getId());
        if (msgs.size() != 0) {
            getMessagesDao().delete(msg);
            application.getDataSourceKernel().onSourceRemoveMessage(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(tl.getId());
        msg.setDate(tl.getDate());
        getMessagesDao().update(msg);
        updateMaxDate(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                application.getDialogSource().getViewSource().invalidateData();
            }
        }
    }

    public synchronized void onMessagePhotoSent(ChatMessage msg, int date, TLLocalPhoto photo) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(photo);
        getMessagesDao().update(msg);
        updateMaxDate(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        saveMedia(msg.getMid(), msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                application.getDialogSource().getViewSource().invalidateData();
            }
        }
    }

    public synchronized void onMessageVideoSent(ChatMessage msg, int date, TLLocalVideo video) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(video);
        getMessagesDao().update(msg);
        updateMaxDate(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        saveMedia(msg.getMid(), msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                application.getDialogSource().getViewSource().invalidateData();
            }
        }
    }

    public synchronized void onMessageSent(ChatMessage msg, int date) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        getMessagesDao().update(msg);
        updateMaxDate(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.SENT);
                description.setDate(msg.getDate());
                description.setTopMessageId(msg.getMid());
                getDialogsDao().update(description);
                application.getDialogSource().getViewSource().updateItem(description);
                application.getDialogSource().getViewSource().invalidateData();
            }
        }
    }

    public synchronized void onMessageFailure(ChatMessage msg) {
        msg.setState(MessageState.FAILURE);
        getMessagesDao().update(msg);
        application.getDataSourceKernel().onSourceUpdateMessage(msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            description.setFailure(true);
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.FAILURE);
            }
            getDialogsDao().update(description);
            application.getDialogSource().getViewSource().updateItem(description);
            application.getDialogSource().getViewSource().invalidateData();
        }
    }

    public void cancelMediaSend(int databaseId) {
        deleteUnsentMessage(databaseId);
    }

    public synchronized void onDeletedOnServer(Integer[] mids) {
        ChatMessage[] messages = getMessagesById(mids);
        for (ChatMessage msg : messages) {
            msg.setDeletedServer(true);
            msg.setDeletedLocal(true);
            getMessagesDao().update(msg);
            application.getDataSourceKernel().onSourceRemoveMessage(msg);
            if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
                deleteMedia(msg.getMid());
            }
            updateDescriptorDeleteSent(msg.getPeerType(), msg.getPeerId(), msg.getMid());
        }
    }

    public synchronized void onRestoredOnServer(Integer[] mids) {
        ChatMessage[] messages = getMessagesById(mids);
        for (ChatMessage msg : messages) {
            msg.setDeletedServer(false);
            msg.setDeletedLocal(false);
            getMessagesDao().update(msg);
            application.getDataSourceKernel().onSourceAddMessage(msg);
            if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
                saveMedia(msg.getMid(), msg);
            }
            updateDescriptorShort(msg);
        }
    }

    public void deleteSentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        msg.setDeletedLocal(true);
        getMessagesDao().update(msg);
        application.getDataSourceKernel().onSourceRemoveMessage(msg);
        if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
            deleteMedia(msg.getMid());
        }
        updateDescriptorDeleteSent(msg.getPeerType(), msg.getPeerId(), msg.getMid());
    }

    public void seldfDestructMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        getMessagesDao().delete(msg);
        application.getDataSourceKernel().onSourceRemoveMessage(msg);
        if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
            deleteMedia(msg.getMid());
        }
        updateDescriptorSelfDestructed(msg.getPeerType(), msg.getPeerId(), msg.getMid());
    }

    public void deleteUnsentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        getMessagesDao().delete(getMessageByDbId(databaseId));
        application.getDataSourceKernel().onSourceRemoveMessage(msg);
        updateDescriptorDeleteUnsent(msg.getPeerType(), msg.getPeerId(), msg.getDatabaseId());
    }

    public synchronized void onLoadMoreMessages(final List<TLAbsMessage> messages, final List<TLAbsUser> users, final List<TLAbsChat> chats) {
        onNewMessages(messages, users, chats, new ArrayList<TLDialog>());
    }

    public synchronized void onLoadMoreDialogs(final List<TLAbsMessage> messages, final List<TLAbsUser> users, final List<TLAbsChat> chats,
                                               final List<TLDialog> dialogs) {
        onNewMessages(messages, users, chats, dialogs);
    }

    public synchronized HashSet<Integer> onNewMessages(final List<TLAbsMessage> messages, final List<TLAbsUser> users, final List<TLAbsChat> chats,
                                                       final List<TLDialog> dialogs) {
        final HashSet<Integer> newUnread = new HashSet<Integer>();
        onUsers(users);
        long start = SystemClock.uptimeMillis();
        Integer[] ids = new Integer[messages.size()];
        ChatMessage[] converted = new ChatMessage[messages.size()];
        for (int i = 0; i < ids.length; i++) {
            converted[i] = EngineUtils.fromTlMessage(messages.get(i), application);
            ids[i] = converted[i].getMid();
            updateMaxDate(converted[i]);
        }

        ChatMessage[] original = getMessagesById(ids);

        final ArrayList<Pair<ChatMessage, ChatMessage>> diff = new ArrayList<Pair<ChatMessage, ChatMessage>>();

        for (ChatMessage m : converted) {
            ChatMessage orig = EngineUtils.searchMessage(original, m.getMid());
            diff.add(new Pair<ChatMessage, ChatMessage>(orig, m));
            /*if (orig == null || m.getMessageId() <= 0) {
                changed.add(m);
                original.add(null);
            } else if (isChatMessagesChanged(orig, m)) {
                changed.add(m);
                original.add(orig);
            }*/
        }

        Logger.d(TAG, "newMessages:prepare time: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        getMessagesDao().callBatchTasks(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (Pair<ChatMessage, ChatMessage> msgs : diff) {
                    ChatMessage orig = msgs.first;
                    ChatMessage changed = msgs.second;

                    if (orig == null) {
                        getMessagesDao().create(changed);
                        if (!changed.isOut() && changed.getState() == MessageState.SENT) {
                            newUnread.add(changed.getMid());
                        }
                        application.getDataSourceKernel().onSourceAddMessage(changed);
                    } else {
                        changed.setDatabaseId(orig.getDatabaseId());
                        getMessagesDao().update(changed);
                        application.getDataSourceKernel().onSourceUpdateMessage(changed);
                    }

                    if (changed.getRawContentType() == ContentType.MESSAGE_PHOTO) {
                        saveMedia(changed.getMid(), changed);
                    } else if (changed.getRawContentType() == ContentType.MESSAGE_VIDEO) {
                        saveMedia(changed.getMid(), changed);
                    }

                    DialogDescription description = getDescriptionForPeer(changed.getPeerType(), changed.getPeerId());
                    if (description == null) {
                        if (changed.getPeerType() == PeerType.PEER_CHAT) {
                            if (changed.getContentType() == ContentType.MESSAGE_SYSTEM && changed.getExtras() instanceof TLLocalActionChatDeleteUser) {
                                TLLocalActionChatDeleteUser user = (TLLocalActionChatDeleteUser) changed.getExtras();
                                if (user.getUserId() == application.getCurrentUid() && changed.getSenderId() == application.getCurrentUid()) {
                                    continue;
                                }
                            }
                        }

                        if (changed.getPeerType() == PeerType.PEER_USER) {
                            description = createDescriptionForUser(changed.getPeerId());
                        } else {
                            TLAbsChat chat = EngineUtils.findChat(chats, changed.getPeerId());
                            description = createDescriptionForChat(changed.getPeerId(), chat, null);
                        }
                        applyDescriptor(description, changed);

                        TLDialog dialog = EngineUtils.findDialog(dialogs, description.getPeerType(), description.getPeerId());
                        if (dialog != null) {
                            description.setUnreadCount(dialog.getUnreadCount());
                        }

                        getDialogsDao().create(description);
                        application.getDialogSource().getViewSource().addItem(description);
                    } else {
                        if (description.getDate() <= changed.getDate()) {
                            applyDescriptor(description, changed);
                            getDialogsDao().update(description);
                            application.getDialogSource().getViewSource().updateItem(description);
                        }
                    }
                }
                return null;
            }
        });

        Logger.d(TAG, "newMessages:update time: " + (SystemClock.uptimeMillis() - start));
        return newUnread;
    }

    // TODO: delete frome specific mid
    public void deleteHistory(int peerType, int peerId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            getDialogsDao().delete(description);
            application.getDialogSource().getViewSource().removeItem(description);
        }

        application.getDataSourceKernel().removeMessageSource(peerType, peerId);

        if (peerType == PeerType.PEER_CHAT) {
            FullChatInfo chatInfo = getFullChatInfo(peerId);
            if (chatInfo != null) {
                getFullChatInfoDao().delete(chatInfo);
            }
        }

        try {
            DeleteBuilder<ChatMessage, Long> deleteBuilder = getMessagesDao().deleteBuilder();
            deleteBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType);
            getMessagesDao().delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            DeleteBuilder<MediaRecord, Long> deleteBuilder = getMediasDao().deleteBuilder();
            deleteBuilder.where().eq("peerId", peerId).and().eq("peerType", peerType);
            getMediasDao().delete(deleteBuilder.prepare());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getMediaCount(int peerType, int peerId) {
        PreparedQuery<MediaRecord> query = null;
        try {
            QueryBuilder<MediaRecord, Long> builder = getMediasDao().queryBuilder();
            if (peerType >= 0) {
                builder.where().eq("peerType", peerType).and().eq("peerId", peerId);
            }
            builder.setCountOf(true);
            query = builder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) getMediasDao().countOf(query);
    }

    public MediaRecord findMedia(int mid) {
        List<MediaRecord> res = getMediasDao().queryForEq("mid", mid);
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
        getMediasDao().create(record);
    }

    public void deleteMedia(int mid) {
        MediaRecord record = findMedia(mid);
        if (record == null)
            return;
        getMediasDao().delete(record);
    }

    public void clearCache() {
        userCache.clear();
    }
}
