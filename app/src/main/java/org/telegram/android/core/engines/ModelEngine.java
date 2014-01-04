package org.telegram.android.core.engines;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.StelsDatabase;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsSentMessage;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLAbsStatedMessages;
import org.telegram.dao.DaoMaster;
import org.telegram.dao.DaoSession;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.mtproto.time.TimeOverlord;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:37
 */
public class ModelEngine {
    private static final String TAG = "ModelEngine";

    private StelsDatabase database;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private StelsApplication application;

    private UsersEngine usersEngine;
    private GroupsEngine groupsEngine;
    private SecretEngine secretEngine;

    private DialogsEngine dialogsEngine;
    private MessagesEngine messagesEngine;
    private MediaEngine mediaEngine;

    public ModelEngine(StelsDatabase database, StelsApplication application) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(application, "users-db", null);
        this.daoMaster = new DaoMaster(helper.getWritableDatabase());
        this.daoMaster.getDatabase().execSQL("PRAGMA synchronous = OFF;");
        this.daoSession = daoMaster.newSession();
        this.database = database;
        this.application = application;
        this.usersEngine = new UsersEngine(this);
        this.secretEngine = new SecretEngine(this);
        this.groupsEngine = new GroupsEngine(this);
        this.mediaEngine = new MediaEngine(this);
        this.dialogsEngine = new DialogsEngine(this);
        this.messagesEngine = new MessagesEngine(this);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public StelsApplication getApplication() {
        return application;
    }

    /**
     * Users actions
     */

    public UsersEngine getUsersEngine() {
        return usersEngine;
    }

    public MediaEngine getMediaEngine() {
        return mediaEngine;
    }

    public DialogsEngine getDialogsEngine() {
        return dialogsEngine;
    }

    public SecretEngine getSecretEngine() {
        return secretEngine;
    }

    public GroupsEngine getGroupsEngine() {
        return groupsEngine;
    }

    public MessagesEngine getMessagesEngine() {
        return messagesEngine;
    }

    public void onUsers(List<TLAbsUser> users) {
        usersEngine.onUsers(users);
    }

    public User getUserRuntime(int id) {
        return usersEngine.getUserRuntime(id);
    }

    public User getUser(int id) {
        return usersEngine.getUser(id);
    }

    public void clearCache() {
        usersEngine.clearCache();
    }

    /**
     * End users actions
     */

    public EncryptedChat getEncryptedChat(int chatId) {
        return secretEngine.getEncryptedChat(chatId);
    }

    public int getMediaCount(int peerType, int peerId) {
        return mediaEngine.getMediaCount(peerType, peerId);
    }

    public MediaRecord findMedia(int mid) {
        return mediaEngine.findMedia(mid);
    }

    public StelsDatabase getDatabase() {
        return database;
    }

    public RuntimeExceptionDao<MediaRecord, Long> getMediasDao() {
        return database.getMediaDao();
    }

    public RuntimeExceptionDao<ChatMessage, Long> getMessagesDao() {
        return database.getMessagesDao();
    }

    public RuntimeExceptionDao<Contact, Long> getContactsDao() {
        return database.getContactsDao();
    }

    public RuntimeExceptionDao<FullChatInfo, Long> getFullChatInfoDao() {
        return database.getFullChatInfoDao();
    }

    public ChatMessage getMessageByDbId(int localId) {
        return messagesEngine.getMessageById(localId);
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

    public void onUpdateMessageId(long rid, int mid) {
        ChatMessage message = messagesEngine.getMessageByRandomId(rid);
        if (message == null) {
            return;
        }
        if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            return;
        }

        if (message.getMid() == mid) {
            return;
        }

        messagesEngine.delete(message);
        dialogsEngine.updateDescriptorDeleteUnsent(message.getPeerType(), message.getPeerId(), message.getDatabaseId());
    }

    public FullChatInfo getFullChatInfo(int chatId) {
        return getFullChatInfoDao().queryForId((long) chatId);
    }

    public DialogDescription getDescriptionForPeer(int peerType, int peerId) {
        return dialogsEngine.loadDialog(peerType, peerId);
    }

    public DialogDescription[] getUnreadedRemotelyDescriptions() {
        return dialogsEngine.getUnreadedRemotelyDescriptions();
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
                dialogsEngine.updateDialog(description);
            }
        }
    }

    public void onNewUnreadEncMessage(int chatId, int date) {
        DialogDescription description = getDescriptionForPeer(PeerType.PEER_USER_ENCRYPTED, chatId);
        if (description != null) {
            if (description.getLastLocalViewedMessage() < date) {
                description.setUnreadCount(description.getUnreadCount() + 1);
                dialogsEngine.updateDialog(description);
            }
        }
    }

    public void onMaxLocalViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setLastLocalViewedMessage(maxId);
            description.setUnreadCount(0);
            dialogsEngine.updateDialog(description);
        }
    }

    public void clearFirstUnreadMessage(int peerType, int peerId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setFirstUnreadMessage(0);
            description.setUnreadCount(0);
            dialogsEngine.updateDialog(description);
        }
    }

    public void onMaxRemoteViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = getDescriptionForPeer(peerType, peerId);
        if (description != null) {
            description.setLastRemoteViewedMessage(maxId);
            dialogsEngine.updateDialog(description);
        }
    }

    public void onChatTitleChanges(int chatId, String title) {
        groupsEngine.onGroupNameChanged(chatId, title);
    }

    public void onChatAvatarChanges(int chatId, TLAbsPhoto photo) {
        groupsEngine.onGroupAvatarChanged(chatId, EngineUtils.convertAvatarPhoto(photo));
    }

    public void onChatUserShortAdded(int chatId, int inviter, int uid) {
        onChatUserAdded(chatId, inviter, uid);
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

        Group group = getGroupsEngine().getGroup(chatId);
        if (group != null) {
            group.setUsersCount(group.getUsersCount() + 1);
            getGroupsEngine().onGroupsUpdated(group);
        }
    }

    public void onChatUserShortRemoved(int chatId, int uid) {
        onChatUserRemoved(chatId, uid);
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
                if (chatInfo.getUids() != null) {
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
        }

        Group group = getGroupsEngine().getGroup(chatId);
        if (group != null) {
            if (uid == application.getCurrentUid()) {
                group.setUsersCount(0);
                group.setForbidden(true);
            } else {
                group.setUsersCount(group.getUsersCount() - 1);
            }
            getGroupsEngine().onGroupsUpdated(group);
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

        Group group = getGroupsEngine().getGroup(chatId);
        if (group != null) {
            if (info.isForbidden()) {
                group.setUsersCount(0);
                group.setForbidden(true);
            } else {
                group.setUsersCount(info.getUids().length);
                group.setForbidden(false);
            }
            getGroupsEngine().onGroupsUpdated(group);
        }
    }

    public void onEncryptedReaded(int chatId, int readDate, int maxDate) {
        try {
            QueryBuilder<ChatMessage, Long> queryBuilder = getMessagesDao().queryBuilder();
            queryBuilder.where()
                    .eq("peerType", PeerType.PEER_USER_ENCRYPTED).and()
                    .eq("peerId", chatId).and()
                    .eq("isOut", true).and()
                    .eq("state", MessageState.SENT).and()
                    .gt("messageTimeout", 0)
                    .le("date", maxDate);

            int localReadDate = (int) (readDate - TimeOverlord.getInstance().getTimeDelta() / 1000);
            for (ChatMessage msg : getMessagesDao().query(queryBuilder.prepare())) {
                msg.setMessageDieTime(localReadDate + msg.getMessageTimeout());
                application.getSelfDestructProcessor().performSelfDestruct(msg.getDatabaseId(), msg.getMessageDieTime());
                markReaded(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onMessagesReaded(int[] mids) {
        ChatMessage[] saved = messagesEngine.getMessagesByMid(mids);
        HashMap<Long, Integer> maxMap = new HashMap<Long, Integer>();
        HashMap<Long, Integer> deletedMap = new HashMap<Long, Integer>();
        for (ChatMessage msg : saved) {
            long id = msg.getPeerType() + msg.getPeerId() * 10L;
            if (msg.isOut()) {
                if (maxMap.containsKey(id)) {
                    if (maxMap.get(id) < msg.getMid()) {
                        maxMap.put(id, msg.getMid());
                    }
                } else {
                    maxMap.put(id, msg.getMid());
                }
            }

            if (!msg.isOut() && msg.getState() == MessageState.SENT) {
                if (deletedMap.containsKey(id)) {
                    deletedMap.put(id, deletedMap.get(id) + 1);
                } else {
                    deletedMap.put(id, 1);
                }
            }
        }

        for (long key : maxMap.keySet()) {
            int peerId = (int) (key / 10);
            int peerType = (int) (key % 10);

            DialogDescription description = getDescriptionForPeer(peerType, peerId);
            if (description != null && description.getTopMessageId() == maxMap.get(key)) {
                description.setMessageState(MessageState.READED);
                dialogsEngine.updateDialog(description);
            }
        }
        for (long key : deletedMap.keySet()) {
            int peerId = (int) (key / 10);
            int peerType = (int) (key % 10);

            DialogDescription description = getDescriptionForPeer(peerType, peerId);
            if (description != null) {
                description.setUnreadCount(Math.max(0, description.getUnreadCount() - deletedMap.get(key)));
                dialogsEngine.updateDialog(description);
            }
        }

        for (int mid : mids) {
            ChatMessage msg = EngineUtils.searchMessage(saved, mid);
            if (msg != null) {
                markReaded(msg);
            }
        }

        messagesEngine.onMessageRead(saved);
    }

    private void markReaded(ChatMessage msg) {
        if (!msg.isOut() && msg.getState() == MessageState.SENT) {
            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null) {
                description.setUnreadCount(Math.max(0, description.getUnreadCount() - 1));
                dialogsEngine.updateDialog(description);
            }
        }
        if (msg.isOut()) {
            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null && description.getTopMessageId() == msg.getMid()) {
                description.setMessageState(MessageState.READED);
                dialogsEngine.updateDialog(description);
            }
        }
        msg.setState(MessageState.READED);
        messagesEngine.update(msg);
    }

    public boolean onNewShortMessage(int peerType, int peerId, int mid, int date, int senderId, String message) {
        if (messagesEngine.getMessageByMid(mid) != null) {
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
        messagesEngine.create(nmsg);
        dialogsEngine.updateDescriptorShort(nmsg);
        return true;
    }

    public boolean onNewShortEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, String message) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
        nmsg.setDate(date);
        nmsg.setMessage(message);
        nmsg.setContentType(ContentType.MESSAGE_TEXT);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        messagesEngine.create(nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public boolean onNewLocationEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, double longitude, double latitude) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
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
        messagesEngine.create(nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public boolean onNewPhotoEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLLocalPhoto photo) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
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
        messagesEngine.create(nmsg);
        mediaEngine.saveMedia(nmsg.getMid(), nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public boolean onNewVideoEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLLocalVideo video) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
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
        messagesEngine.create(nmsg);
        mediaEngine.saveMedia(nmsg.getMid(), nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public boolean onNewDocEncMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLLocalDocument doc) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
        nmsg.setDate(date);
        nmsg.setMessage("Document");
        nmsg.setContentType(ContentType.MESSAGE_DOCUMENT);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(doc);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        messagesEngine.create(nmsg);
        mediaEngine.saveMedia(nmsg.getMid(), nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public void onNewInternalServiceMessage(int peerType, int peerId, int senderId, int date, TLAbsLocalAction action) {
        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
        nmsg.setDate(date);
        nmsg.setMessage("");
        nmsg.setContentType(ContentType.MESSAGE_SYSTEM);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setExtras(action);
        nmsg.setOut(senderId == application.getCurrentUid());
        messagesEngine.create(nmsg);
        dialogsEngine.updateDescriptorShort(nmsg);
    }

    // Message sending

    private ChatMessage prepareSendMessage(int peerType, int peerId) {
        ChatMessage msg = new ChatMessage();
        msg.setMid(messagesEngine.generateMid());
        msg.setPeerId(peerId);
        msg.setPeerType(peerType);
        msg.setOut(true);
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = messagesEngine.getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setSenderId(application.getCurrentUid());
        msg.setState(MessageState.PENDING);
        msg.setRandomId(Entropy.generateRandomId());
        return msg;
    }

    private ChatMessage prepareSendMessageAsync(int peerType, int peerId) {
        ChatMessage msg = new ChatMessage();
        msg.setPeerId(peerId);
        msg.setPeerType(peerType);
        msg.setOut(true);
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = messagesEngine.getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setSenderId(application.getCurrentUid());
        msg.setState(MessageState.PENDING);
        msg.setRandomId(Entropy.generateRandomId());
        return msg;
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

    public int sendDocument(int peerType, int peerId, String fileName) {
        TLUploadingDocument document = new TLUploadingDocument(fileName, (int) new File(fileName).length());
        return sendDocument(peerType, peerId, document);
    }

    private int sendDocument(int peerType, int peerId, TLUploadingDocument doc) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Document");
        msg.setContentType(ContentType.MESSAGE_DOCUMENT);
        msg.setExtras(doc);
        messagesEngine.create(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    private int sendPhoto(int peerType, int peerId, TLUploadingPhoto photo) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Photo");
        msg.setContentType(ContentType.MESSAGE_PHOTO);
        msg.setExtras(photo);
        messagesEngine.create(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    private int sendVideo(int peerType, int peerId, TLUploadingVideo video) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Video");
        msg.setContentType(ContentType.MESSAGE_VIDEO);
        msg.setExtras(video);
        messagesEngine.create(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int forwardMessage(int peerType, int peerId, int mid) {
        ChatMessage src = messagesEngine.getMessageByMid(mid);

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
        } else if (src.getRawContentType() == ContentType.MESSAGE_GEO) {
            msg.setContentType(ContentType.MESSAGE_DOCUMENT | ContentType.MESSAGE_FORWARDED);
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
        messagesEngine.create(msg);
        application.getMessageSender().sendMessage(msg);

        dialogsEngine.updateDescriptorPending(msg);
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

        messagesEngine.create(msg);
        application.getMessageSender().sendMessage(msg);

        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int sendLocation(int peerType, int peerId, TLLocalGeo point) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("");
        msg.setContentType(ContentType.MESSAGE_GEO);
        msg.setExtras(point);
        messagesEngine.create(msg);
        application.getMessageSender().sendMessage(msg);
        dialogsEngine.updateDescriptorPending(msg);
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
        msg.setMid(messagesEngine.generateMid());
        application.getDataSourceKernel().onSourceRemoveMessage(msg);
        messagesEngine.create(msg);
        dialogsEngine.updateDescriptorPending(msg);
    }


    public int sendMessage(int peerType, int peerId, String message) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage(message);
        msg.setContentType(ContentType.MESSAGE_TEXT);
        messagesEngine.create(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public void tryAgainMedia(int localId) {
        ChatMessage msg = getMessageByDbId(localId);
        msg.setMid(messagesEngine.generateMid());
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = messagesEngine.getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setState(MessageState.PENDING);
        msg.setRandomId(Entropy.generateRandomId());
        messagesEngine.update(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
    }

    public void tryAgain(int localId) {
        ChatMessage msg = getMessageByDbId(localId);
        msg.setMid(messagesEngine.generateMid());
        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);
        int maxDate = messagesEngine.getMaxDate();
        if (date < maxDate) {
            date = maxDate + 1;
        }
        msg.setDate(date);
        msg.setState(MessageState.PENDING);
        msg.setRandomId(Entropy.generateRandomId());
        messagesEngine.update(msg);
        application.getMessageSender().sendMessage(msg);
        dialogsEngine.updateDescriptorPending(msg);
    }

    public synchronized void onConfirmed(ChatMessage msg) {
        if (msg.getState() == MessageState.PENDING) {
            msg.setState(MessageState.SENT);
            messagesEngine.update(msg);

            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
            if (description != null) {
                if (description.getTopMessageId() == -msg.getDatabaseId()) {
                    description.setMessageState(MessageState.SENT);
                    description.setDate(msg.getDate());
                    dialogsEngine.updateDialog(description);
                }
            }
        }
    }

    public synchronized void onForwarded(ChatMessage msg, TLAbsStatedMessages statedMessages) {
        int mid = statedMessages.getMessages().get(0).getId();

        if (getMessagesEngine().getMessageByMid(mid) != null) {
            messagesEngine.delete(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);

        messagesEngine.update(msg);
        onUsers(statedMessages.getUsers());
        groupsEngine.onGroupsUpdated(statedMessages.getChats());

        onNewMessages(statedMessages.getMessages(), new ArrayList<TLDialog>());

        dialogsEngine.updateDescriptorSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), mid, msg.getDatabaseId());
    }

    public synchronized void onMessageSent(ChatMessage msg, TLAbsStatedMessage statedMessage) {
        int mid = statedMessage.getMessage().getId();
        if (getMessagesEngine().getMessageByMid(mid) != null) {
            messagesEngine.delete(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);
        messagesEngine.update(msg);
        List<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
        messages.add(statedMessage.getMessage());
        onUsers(statedMessage.getUsers());
        groupsEngine.onGroupsUpdated(statedMessage.getChats());
        onNewMessages(messages, new ArrayList<TLDialog>());
        dialogsEngine.updateDescriptorSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), mid, msg.getDatabaseId());
    }

    public synchronized void onMessageSent(ChatMessage msg, TLAbsSentMessage tl) {
        if (getMessagesEngine().getMessageByMid(tl.getId()) != null) {
            messagesEngine.delete(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(tl.getId());
        msg.setDate(tl.getDate());
        messagesEngine.update(msg);
        dialogsEngine.updateDescriptorSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), tl.getId(), msg.getDatabaseId());
    }

    public synchronized void onMessageEncPhotoSent(ChatMessage msg, int date, TLLocalPhoto photo) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(photo);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg.getMid(), msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public synchronized void onMessageEncDocSent(ChatMessage msg, int date, TLLocalDocument doc) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(doc);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg.getMid(), msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public synchronized void onMessagePhotoSent(ChatMessage msg, int date, int mid, TLLocalPhoto photo) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setMid(mid);
        msg.setExtras(photo);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg.getMid(), msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public synchronized void onMessageVideoSent(ChatMessage msg, int date, TLLocalVideo video) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(video);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg.getMid(), msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public synchronized void onMessageSent(ChatMessage msg, int date) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        messagesEngine.update(msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public synchronized void onMessageFailure(ChatMessage msg) {
        if (getMessageByDbId(msg.getDatabaseId()) == null) {
            return;
        }
        msg.setState(MessageState.FAILURE);
        messagesEngine.update(msg);
        DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            description.setFailure(true);
            if (description.getTopMessageId() == -msg.getDatabaseId()) {
                description.setMessageState(MessageState.FAILURE);
            }
            dialogsEngine.updateDialog(description);
        }
    }

    public synchronized void cancelMediaSend(int databaseId) {
        deleteUnsentMessage(databaseId);
    }

    public synchronized void onDeletedOnServer(int[] mids) {
        messagesEngine.onDeletedOnServer(mids);

        ChatMessage[] messages = messagesEngine.getMessagesByMid(mids);
        for (ChatMessage msg : messages) {
            if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
                mediaEngine.deleteMedia(msg.getMid());
            }
            dialogsEngine.updateDescriptorDeleteSent(msg.getPeerType(), msg.getPeerId(), msg.getMid());
        }
    }

    public synchronized void onRestoredOnServer(int[] mids) {
        messagesEngine.onRestoredOnServer(mids);

        ChatMessage[] messages = messagesEngine.getMessagesByMid(mids);
        for (ChatMessage msg : messages) {
            if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
                mediaEngine.saveMedia(msg.getMid(), msg);
            }
            dialogsEngine.updateDescriptorShort(msg);
        }
    }

    public void deleteSentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        msg.setDeletedLocal(true);
        messagesEngine.delete(msg);
        if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
            mediaEngine.deleteMedia(msg.getMid());
        }
        dialogsEngine.updateDescriptorDeleteSent(msg.getPeerType(), msg.getPeerId(), msg.getMid());
    }

    public void selfDestructMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        messagesEngine.delete(msg);
        if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
            mediaEngine.deleteMedia(msg.getMid());
        }
        dialogsEngine.updateDescriptorSelfDestructed(msg.getPeerType(), msg.getPeerId(), msg.getMid());
    }

    public void deleteUnsentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        messagesEngine.delete(msg);
        dialogsEngine.updateDescriptorDeleteUnsent(msg.getPeerType(), msg.getPeerId(), msg.getDatabaseId());
    }

    public void onLoadMoreMessages(final List<TLAbsMessage> messages) {
        messagesEngine.onLoadMoreMessages(messages);
    }

    public void onLoadMoreDialogs(final List<TLAbsMessage> messages, final List<TLDialog> dialogs) {
        onNewMessages(messages, dialogs);
    }

    public HashSet<Integer> onNewMessages(final List<TLAbsMessage> messages,
                                          final List<TLDialog> dialogs) {
        final HashSet<Integer> newUnread = new HashSet<Integer>();
        long start = SystemClock.uptimeMillis();
        int[] ids = new int[messages.size()];
        ChatMessage[] converted = new ChatMessage[messages.size()];
        for (int i = 0; i < ids.length; i++) {
            converted[i] = EngineUtils.fromTlMessage(messages.get(i), application);
            ids[i] = converted[i].getMid();
        }

        ChatMessage[] original = messagesEngine.getMessagesByMid(ids);

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
                    // Logger.d(TAG, "newMessages: " + msgs.second.getMid());
                    ChatMessage orig = msgs.first;
                    ChatMessage changed = msgs.second;

                    if (orig == null) {
                        messagesEngine.create(changed);
                        if (!changed.isOut() && changed.getState() == MessageState.SENT) {
                            newUnread.add(changed.getMid());
                        }
                    } else {
                        changed.setDatabaseId(orig.getDatabaseId());
                        messagesEngine.update(changed);
                    }

                    if (changed.getRawContentType() == ContentType.MESSAGE_PHOTO) {
                        mediaEngine.saveMedia(changed.getMid(), changed);
                    } else if (changed.getRawContentType() == ContentType.MESSAGE_VIDEO) {
                        mediaEngine.saveMedia(changed.getMid(), changed);
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
                            description = new DialogDescription(PeerType.PEER_USER, changed.getPeerId());
                        } else {
                            description = new DialogDescription(PeerType.PEER_CHAT, changed.getPeerId());
                        }
                        dialogsEngine.applyDescriptor(description, changed);

                        TLDialog dialog = EngineUtils.findDialog(dialogs, description.getPeerType(), description.getPeerId());
                        if (dialog != null) {
                            description.setUnreadCount(dialog.getUnreadCount());
                        }

                        dialogsEngine.updateOrCreateDialog(description);
                    } else {
                        if (description.getDate() <= changed.getDate()) {
                            dialogsEngine.applyDescriptor(description, changed);
                            dialogsEngine.updateDialog(description);
                        }
                    }
                }
                Logger.d(TAG, "newMessages:ended");
                return null;
            }
        });

        Logger.d(TAG, "newMessages:update time: " + (SystemClock.uptimeMillis() - start));
        return newUnread;
    }

    public void deleteHistory(int peerType, int peerId) {
        if (peerType == PeerType.PEER_CHAT) {
            FullChatInfo chatInfo = getFullChatInfo(peerId);
            if (chatInfo != null) {
                getFullChatInfoDao().delete(chatInfo);
            }
            groupsEngine.deleteGroup(peerId);
        }
        dialogsEngine.deleteDialog(peerType, peerId);
        messagesEngine.deleteHistory(peerType, peerId);
        mediaEngine.deleteMediaFromChat(peerType, peerId);
    }
}
