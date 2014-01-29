package org.telegram.android.core.engines;

import android.net.Uri;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.messages.TLAbsSentMessage;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLAbsStatedMessages;
import org.telegram.dao.*;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.TLObject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:37
 */
public class ModelEngine {
    private static final String TAG = "ModelEngine";

    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private TelegramApplication application;

    private UsersEngine usersEngine;
    private GroupsEngine groupsEngine;
    private SecretEngine secretEngine;

    private FullGroupEngine fullGroupEngine;

    private DialogsEngine dialogsEngine;
    private MessagesEngine messagesEngine;
    private MediaEngine mediaEngine;

    private SyncStateEngine syncStateEngine;

    public ModelEngine(TelegramApplication application) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(application, "telegram.db", null);
        this.daoMaster = new DaoMaster(helper.getWritableDatabase());
        this.daoMaster.getDatabase().execSQL("PRAGMA synchronous = OFF;");
        this.daoSession = daoMaster.newSession();
        this.application = application;
        this.usersEngine = new UsersEngine(this);
        this.secretEngine = new SecretEngine(this);
        this.groupsEngine = new GroupsEngine(this);
        this.mediaEngine = new MediaEngine(this);
        this.dialogsEngine = new DialogsEngine(this);
        this.messagesEngine = new MessagesEngine(this);
        this.fullGroupEngine = new FullGroupEngine(this);
        this.syncStateEngine = new SyncStateEngine(application);
    }

    public DaoSession getDaoSession() {
        return daoSession;
    }

    public TelegramApplication getApplication() {
        return application;
    }

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

    public FullGroupEngine getFullGroupEngine() {
        return fullGroupEngine;
    }

    public SyncStateEngine getSyncStateEngine() {
        return syncStateEngine;
    }

    /**
     * Users actions
     */

    public void onUsers(List<TLAbsUser> users) {
        usersEngine.onUsers(users);
    }

    public User getUserRuntime(int id) {
        return usersEngine.getUserRuntime(id);
    }

    public User getUser(int id) {
        return usersEngine.getUser(id);
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

    public ChatMessage getMessageByDbId(int localId) {
        return messagesEngine.getMessageById(localId);
    }

    public void markUnsentAsFailured() {
        for (ChatMessage message : messagesEngine.getUnsentMessages()) {
            onMessageFailure(message);
        }
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
        return fullGroupEngine.loadFullChatInfo(chatId);
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

    public void onChatTitleChanges(int chatId, String title) {
        groupsEngine.onGroupNameChanged(chatId, title);
    }

    public void onChatAvatarChanges(int chatId, TLAbsLocalAvatarPhoto photo) {
        groupsEngine.onGroupAvatarChanged(chatId, photo);
    }

    public void onEncryptedReaded(int chatId, int readDate, int maxDate) {
        int localReadDate = (int) (readDate - TimeOverlord.getInstance().getTimeDelta() / 1000);

        ChatMessage[] unreadMessage = messagesEngine.getUnreadSecret(chatId, maxDate);

        ArrayList<ChatMessage> pendingSelfDestruct = new ArrayList<ChatMessage>();
        for (ChatMessage msg : unreadMessage) {
            if (msg.getMessageTimeout() > 0) {
                msg.setMessageDieTime(localReadDate + msg.getMessageTimeout());
                pendingSelfDestruct.add(msg);
            }
        }

        onMessagesReaded(unreadMessage);

        for (ChatMessage msg : pendingSelfDestruct) {
            application.getSelfDestructProcessor().performSelfDestruct(msg.getDatabaseId(), msg.getMessageDieTime());
        }
    }

    public void onMessagesReaded(ChatMessage[] messages) {
        // Loading dialogs
        HashSet<Long> dialogIds = new HashSet<Long>();
        HashMap<Long, DialogDescription> dialogs = new HashMap<Long, DialogDescription>();
        HashSet<DialogDescription> updatedDescriptions = new HashSet<DialogDescription>();

        for (ChatMessage msg : messages) {
            long id = msg.getPeerType() + msg.getPeerId() * 10L;
            dialogIds.add(id);
        }

        DialogDescription[] tDescriptions = dialogsEngine.loadDialogs(dialogIds.toArray(new Long[0]));
        for (DialogDescription d : tDescriptions) {
            dialogs.put(d.getUniqId(), d);
        }
        HashMap<Long, Integer> deletedMap = new HashMap<Long, Integer>();
        for (ChatMessage msg : messages) {
            long id = msg.getPeerType() + msg.getPeerId() * 10L;
            if (!msg.isOut() && msg.getState() == MessageState.SENT) {
                if (deletedMap.containsKey(id)) {
                    deletedMap.put(id, deletedMap.get(id) + 1);
                } else {
                    deletedMap.put(id, 1);
                }
            }

            DialogDescription description = dialogs.get(id);
            if (description != null) {
                if (description.getTopMessageId() == -msg.getDatabaseId() || description.getTopMessageId() == msg.getMid()) {
                    description.setMessageState(MessageState.READED);
                    updatedDescriptions.add(description);
                }
            }

            msg.setState(MessageState.READED);
        }
        for (long key : deletedMap.keySet()) {
            int peerType = (int) ((10 + key % 10L) % 10);
            int peerId = (int) ((key - peerType) / 10L);

            DialogDescription description = getDescriptionForPeer(peerType, peerId);
            if (description != null) {
                description.setUnreadCount(Math.max(0, description.getUnreadCount() - deletedMap.get(key)));
                updatedDescriptions.add(description);
            }
        }

        dialogsEngine.updateOrCreateDialog(updatedDescriptions.toArray(new DialogDescription[updatedDescriptions.size()]));
        messagesEngine.onMessageRead(messages);
    }

    public void onMessagesReaded(int[] mids) {
        ChatMessage[] saved = messagesEngine.getMessagesByMid(mids);
        onMessagesReaded(saved);
//        HashMap<Long, Integer> maxMap = new HashMap<Long, Integer>();
//        HashMap<Long, Integer> deletedMap = new HashMap<Long, Integer>();
//        for (ChatMessage msg : saved) {
//            long id = msg.getPeerType() + msg.getPeerId() * 10L;
//            if (msg.isOut()) {
//                if (maxMap.containsKey(id)) {
//                    if (maxMap.get(id) < msg.getMid()) {
//                        maxMap.put(id, msg.getMid());
//                    }
//                } else {
//                    maxMap.put(id, msg.getMid());
//                }
//            }
//
//            if (!msg.isOut() && msg.getState() == MessageState.SENT) {
//                if (deletedMap.containsKey(id)) {
//                    deletedMap.put(id, deletedMap.get(id) + 1);
//                } else {
//                    deletedMap.put(id, 1);
//                }
//            }
//        }
//
//        for (long key : maxMap.keySet()) {
//            int peerId = (int) (key / 10);
//            int peerType = (int) (key % 10);
//
//            DialogDescription description = getDescriptionForPeer(peerType, peerId);
//            if (description != null && description.getTopMessageId() == maxMap.get(key)) {
//                description.setMessageState(MessageState.READED);
//                dialogsEngine.updateDialog(description);
//            }
//        }
//        for (long key : deletedMap.keySet()) {
//            int peerId = (int) (key / 10);
//            int peerType = (int) (key % 10);
//
//            DialogDescription description = getDescriptionForPeer(peerType, peerId);
//            if (description != null) {
//                description.setUnreadCount(Math.max(0, description.getUnreadCount() - deletedMap.get(key)));
//                dialogsEngine.updateDialog(description);
//            }
//        }

//        for (int mid : mids) {
//            ChatMessage msg = EngineUtils.searchMessage(saved, mid);
//            if (msg != null) {
//                markReaded(msg);
//            }
//        }

//        messagesEngine.onMessageRead(saved);
    }

//    private void markReaded(ChatMessage msg) {
//        if (!msg.isOut() && msg.getState() == MessageState.SENT) {
//            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
//            if (description != null) {
//                description.setUnreadCount(Math.max(0, description.getUnreadCount() - 1));
//                dialogsEngine.updateDialog(description);
//            }
//        }
//        if (msg.isOut()) {
//            DialogDescription description = getDescriptionForPeer(msg.getPeerType(), msg.getPeerId());
//            if (description != null && description.getTopMessageId() == msg.getMid()) {
//                description.setMessageState(MessageState.READED);
//                dialogsEngine.updateDialog(description);
//            }
//        }
//        msg.setState(MessageState.READED);
//        messagesEngine.update(msg);
//    }

    public boolean onNewSecretMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, String message) {
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

    public boolean onNewSecretMediaMessage(int peerType, int peerId, long randomId, int date, int senderId, int timeout, TLObject media) {
        if (messagesEngine.getMessageByRandomId(randomId) != null) {
            return false;
        }

        ChatMessage nmsg = new ChatMessage();
        nmsg.setMid(messagesEngine.generateMid());
        nmsg.setDate(date);
        nmsg.setExtras(media);
        nmsg.setPeerId(peerId);
        nmsg.setPeerType(peerType);
        if (media instanceof TLLocalGeo) {
            nmsg.setMessage("Geo");
            nmsg.setContentType(ContentType.MESSAGE_GEO);
        } else if (media instanceof TLLocalPhoto) {
            nmsg.setMessage("Photo");
            nmsg.setContentType(ContentType.MESSAGE_PHOTO);
        } else if (media instanceof TLLocalVideo) {
            nmsg.setMessage("Video");
            nmsg.setContentType(ContentType.MESSAGE_VIDEO);
        } else if (media instanceof TLLocalDocument) {
            nmsg.setMessage("Document");

            TLLocalDocument document = (TLLocalDocument) media;
            if (document.getFastPreview().length > 0) {
                if (document.getMimeType().equals("image/gif")) {
                    nmsg.setContentType(ContentType.MESSAGE_DOC_ANIMATED);
                } else {
                    nmsg.setContentType(ContentType.MESSAGE_DOC_PREVIEW);
                }
            } else {
                nmsg.setContentType(ContentType.MESSAGE_DOCUMENT);
            }
        } else {
            nmsg.setMessage("Unknown");
            nmsg.setContentType(ContentType.MESSAGE_UNKNOWN);
        }

        nmsg.setState(MessageState.SENT);
        nmsg.setSenderId(senderId);
        nmsg.setRandomId(randomId);
        nmsg.setMessageTimeout(timeout);
        messagesEngine.create(nmsg);
        dialogsEngine.updateDescriptorShortEnc(nmsg);
        return true;
    }

    public boolean onNewMessage(int peerType, int peerId, int mid, int date, int senderId, String message) {
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
        msg.setExtras(new TLLocalEmpty());
        return msg;
    }

    public int sendDocument(int peerType, int peerId, TLUploadingDocument doc) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("");
        if (doc.getKind() == TLUploadingDocument.KIND_PHOTO) {
            msg.setContentType(ContentType.MESSAGE_DOC_PREVIEW);
        } else if (doc.getKind() == TLUploadingDocument.KIND_GIF) {
            msg.setContentType(ContentType.MESSAGE_DOC_ANIMATED);
        } else {
            if (doc.getMimeType().equals("application/ogg")
                    || doc.getMimeType().equals("audio/ogg")
                    || doc.getMimeType().equals("audio/mp4")
                    || doc.getMimeType().equals("audio/mpeg")
                    || doc.getMimeType().equals("audio/vorbis")) {
                msg.setContentType(ContentType.MESSAGE_AUDIO);
            } else {
                msg.setContentType(ContentType.MESSAGE_DOCUMENT);
            }
        }

        msg.setExtras(doc);
        messagesEngine.create(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int sendPhoto(int peerType, int peerId, int w, int h, Uri uri) {
        try {
            InputStream inputStream = application.getContentResolver().openInputStream(uri);
            char a = (char) inputStream.read();
            char b = (char) inputStream.read();
            char c = (char) inputStream.read();
            if (a == 'G' && b == 'I' && c == 'F') {
                return sendDocument(peerType, peerId, new TLUploadingDocument(uri.toString(), application));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendPhoto(peerType, peerId, new TLUploadingPhoto(w, h, uri));
    }

    public int sendPhoto(int peerType, int peerId, int w, int h, String fileName) {
        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            char a = (char) inputStream.read();
            char b = (char) inputStream.read();
            char c = (char) inputStream.read();
            if (a == 'G' && b == 'I' && c == 'F') {
                return sendDocument(peerType, peerId, new TLUploadingDocument(fileName));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sendPhoto(peerType, peerId, new TLUploadingPhoto(w, h, fileName));
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

    public int sendVideo(int peerType, int peerId, TLUploadingVideo video) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Video");
        msg.setContentType(ContentType.MESSAGE_VIDEO);
        msg.setExtras(video);
        messagesEngine.create(msg);
        application.getMediaSender().sendMedia(msg);
        dialogsEngine.updateDescriptorPending(msg);
        return msg.getDatabaseId();
    }

    public int sendAudio(int peerType, int peerId, TLUploadingAudio audio) {
        ChatMessage msg = prepareSendMessage(peerType, peerId);
        msg.setMessage("Video");
        msg.setContentType(ContentType.MESSAGE_AUDIO);
        msg.setExtras(audio);
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
        } else if (src.getRawContentType() == ContentType.MESSAGE_DOCUMENT) {
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
        ChatMessage msg = prepareSendMessage(peerType, peerId);
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

    public void onConfirmed(ChatMessage msg) {
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

    public void onForwarded(ChatMessage msg, TLAbsStatedMessages statedMessages) {
        int mid = statedMessages.getMessages().get(0).getId();

        if (getMessagesEngine().getMessageByMid(mid) != null) {
            messagesEngine.delete(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);

        messagesEngine.update(msg);
        usersEngine.onUsers(statedMessages.getUsers());
        groupsEngine.onGroupsUpdated(statedMessages.getChats());
        onUpdatedMessages(statedMessages.getMessages());

        dialogsEngine.updateDescriptorSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), mid, msg.getDatabaseId());
    }

    public void onMessageSent(ChatMessage msg, TLAbsStatedMessage statedMessage) {
        int mid = statedMessage.getMessage().getId();
        if (getMessagesEngine().getMessageByMid(mid) != null) {
            messagesEngine.delete(msg);
            return;
        }

        msg.setState(MessageState.SENT);
        msg.setMid(mid);
        messagesEngine.update(msg);
        usersEngine.onUsers(statedMessage.getUsers());
        groupsEngine.onGroupsUpdated(statedMessage.getChats());
        onUpdatedMessage(statedMessage.getMessage());
        dialogsEngine.updateDescriptorSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), mid, msg.getDatabaseId());
    }

    public void onMessageSent(ChatMessage msg, TLAbsSentMessage tl) {
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

    public void onMessageSecretMediaSent(ChatMessage msg, int date, TLObject media) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setExtras(media);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }


    public void onMessagePhotoSent(ChatMessage msg, int date, int mid, TLLocalPhoto photo) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setMid(mid);
        msg.setExtras(photo);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public void onMessageDocSent(ChatMessage msg, int date, int mid, TLLocalDocument doc) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        msg.setMid(mid);
        msg.setExtras(doc);
        messagesEngine.update(msg);
        mediaEngine.saveMedia(msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public void onMessageSent(ChatMessage msg, int date) {
        msg.setState(MessageState.SENT);
        msg.setDate(date);
        messagesEngine.update(msg);
        dialogsEngine.updateDescriptorEncSent(msg.getPeerType(), msg.getPeerId(), msg.getDate(), msg.getDatabaseId());
    }

    public void onMessageFailure(ChatMessage msg) {
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

    public void cancelMediaSend(int databaseId) {
        deleteUnsentMessage(databaseId);
    }

    public void onDeletedOnServer(int[] mids) {
        messagesEngine.onDeletedOnServer(mids);

        ChatMessage[] messages = messagesEngine.getMessagesByMid(mids);
        for (ChatMessage msg : messages) {
            if (msg.getExtras() instanceof TLLocalPhoto || msg.getExtras() instanceof TLLocalVideo) {
                mediaEngine.deleteMedia(msg.getMid());
            }
            dialogsEngine.updateDescriptorDeleteSent(msg.getPeerType(), msg.getPeerId(), msg.getMid());
        }
    }

    public void onRestoredOnServer(int[] mids) {
        messagesEngine.onRestoredOnServer(mids);

        ChatMessage[] messages = messagesEngine.getMessagesByMid(mids);
        mediaEngine.saveMedia(messages);
        for (ChatMessage msg : messages) {
            dialogsEngine.updateDescriptorShort(msg);
        }
    }

    public void deleteSentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        msg.setDeletedLocal(true);
        messagesEngine.delete(msg);
        mediaEngine.deleteMedia(msg.getMid());
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
        dialogsEngine.updateDescriptorSelfDestructed(msg.getPeerType(), msg.getPeerId(), msg.getMid(), msg.getDatabaseId());
    }

    public void deleteUnsentMessage(int databaseId) {
        ChatMessage msg = getMessageByDbId(databaseId);
        if (msg == null)
            return;
        messagesEngine.delete(msg);
        dialogsEngine.updateDescriptorDeleteUnsent(msg.getPeerType(), msg.getPeerId(), msg.getDatabaseId());
    }

    public void onLoadMoreMessages(final List<TLAbsMessage> messages) {
        messagesEngine.updateMessages(messages);
    }

    public void onLoadMoreDialogs(final List<TLAbsMessage> messages, final List<TLDialog> dialogs) {
        ChatMessage[] res = messagesEngine.updateMessages(messages);
        dialogsEngine.updateDescriptors(res, dialogs);
    }

    public void onUpdatedMessage(TLAbsMessage message) {
        ArrayList<TLAbsMessage> messages = new ArrayList<TLAbsMessage>();
        messages.add(message);
        onUpdatedMessages(messages);
    }

    public ChatMessage[] onUpdatedMessages(final List<TLAbsMessage> messages) {
        ChatMessage[] res = messagesEngine.updateMessages(messages);
        dialogsEngine.updateDescriptors(res, null);
        return res;
    }

    public void deleteHistory(int peerType, int peerId) {
        if (peerType == PeerType.PEER_CHAT) {
            fullGroupEngine.delete(peerId);
            groupsEngine.deleteGroup(peerId);
        }
        dialogsEngine.deleteDialog(peerType, peerId);
        messagesEngine.deleteHistory(peerType, peerId);
        mediaEngine.deleteMediaFromChat(peerType, peerId);
    }

    public void dropData() {
        usersEngine.clear();
        groupsEngine.clear();
        fullGroupEngine.clear();
        secretEngine.clear();

        dialogsEngine.clear();
        messagesEngine.clear();

        mediaEngine.clear();

        syncStateEngine.clear();
    }
}
