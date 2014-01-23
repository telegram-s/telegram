package org.telegram.android.core.engines;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusEmpty;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalEmpty;
import org.telegram.android.kernel.ApplicationKernel;
import org.telegram.android.kernel.StorageKernel;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 20.01.14.
 */
public class ModelUpgrader {
    public static void performUpgrade(StorageKernel storageKernel, ApplicationKernel kernel) {
         File databasePath = kernel.getApplication().getDatabasePath("stels.db");
//        File databasePath = new File("/sdcard/stels.db");
        if (databasePath.exists()) {
            SQLiteDatabase database = null;
            try {
                database = SQLiteDatabase.openDatabase(databasePath.toString(), null, SQLiteDatabase.OPEN_READONLY);
                storageKernel.getModel().dropData();
                int dbVersion = database.getVersion();
                if (dbVersion < 47) {
                    return;
                }

                Cursor cursor = database.query("dialogdescription", null, null, null, null, null, null);
                ArrayList<OldDialog> dialogs = new ArrayList<OldDialog>();
                while (cursor.moveToNext()) {
                    OldDialog dialog = new OldDialog();
                    dialog.setPeerType(cursor.getInt(cursor.getColumnIndex("peerType")));
                    dialog.setPeerId(cursor.getInt(cursor.getColumnIndex("peerId")));
                    dialog.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    dialog.setPhoto(cursor.getBlob(cursor.getColumnIndex("photo")));
                    dialog.setUnreadCount(cursor.getInt(cursor.getColumnIndex("unreadCount")));
                    dialog.setParticipantsCount(cursor.getInt(cursor.getColumnIndex("participantsCount")));
                    dialog.setDate(cursor.getInt(cursor.getColumnIndex("date")));
                    dialog.setTopMessageId(cursor.getInt(cursor.getColumnIndex("topMessageId")));
                    dialog.setContentId(cursor.getInt(cursor.getColumnIndex("contentType")));
                    dialog.setMessage(cursor.getString(cursor.getColumnIndex("message")));
                    dialog.setMessageState(cursor.getInt(cursor.getColumnIndex("messageState")));
                    dialog.setLastLocalViewedMessage(cursor.getInt(cursor.getColumnIndex("lastLocalViewedMessage")));
                    dialog.setLastRemoteViewedMessage(cursor.getInt(cursor.getColumnIndex("lastRemoteViewedMessage")));
                    dialog.setFailureFlag(cursor.getInt(cursor.getColumnIndex("failure")) > 0);
                    dialog.setSenderId(cursor.getInt(cursor.getColumnIndex("senderId")));
                    if (dbVersion < 53) {
                        dialog.setFirstUnreadMessage(0);
                    } else {
                        dialog.setFirstUnreadMessage(cursor.getLong(cursor.getColumnIndex("firstUnreadMessage")));
                    }
                    dialog.setExtras(cursor.getBlob(cursor.getColumnIndex("extras")));
                    dialogs.add(dialog);
                }
                cursor.close();

                cursor = database.query("user", null, null, null, null, null, null);
                ArrayList<OldUser> users = new ArrayList<OldUser>();
                while (cursor.moveToNext()) {
                    OldUser user = new OldUser();
                    user.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
                    user.setFirstName(cursor.getString(cursor.getColumnIndex("firstName")));
                    user.setLastName(cursor.getString(cursor.getColumnIndex("lastName")));
                    user.setAccessHash(cursor.getLong(cursor.getColumnIndex("accessHash")));
                    user.setPhone(cursor.getString(cursor.getColumnIndex("phone")));
                    user.setPhoto(cursor.getBlob(cursor.getColumnIndex("photo")));
                    user.setStatus(cursor.getBlob(cursor.getColumnIndex("status")));
                    user.setLinkType(cursor.getInt(cursor.getColumnIndex("linkType")));
                    users.add(user);
                }
                cursor.close();

                ArrayList<OldEncryptedChat> encryptedChats = new ArrayList<OldEncryptedChat>();
                cursor = database.query("encryptedchat", null, null, null, null, null, null);
                while (cursor.moveToNext()) {
                    OldEncryptedChat encryptedChat = new OldEncryptedChat();
                    encryptedChat.setId(cursor.getInt(cursor.getColumnIndex("_id")));
                    encryptedChat.setAccessHash(cursor.getLong(cursor.getColumnIndex("accessHash")));
                    encryptedChat.setUid(cursor.getInt(cursor.getColumnIndex("userId")));
                    encryptedChat.setState(cursor.getInt(cursor.getColumnIndex("state")));
                    encryptedChat.setKey(cursor.getBlob(cursor.getColumnIndex("key")));
                    encryptedChat.setSelfDestruct(cursor.getInt(cursor.getColumnIndex("selfDestructTime")));
                    if (dbVersion <= 47) {
                        encryptedChat.setOut(true);
                    } else {
                        encryptedChat.setOut(cursor.getInt(cursor.getColumnIndex("isOut")) > 0);
                    }
                    encryptedChats.add(encryptedChat);
                }
                cursor.close();

                ArrayList<OldMessage> oldMessages = new ArrayList<OldMessage>();
                for (OldEncryptedChat encryptedChat : encryptedChats) {
                    cursor = database.query("chatmessage", null, "peerId = ? AND peerType = " + PeerType.PEER_USER_ENCRYPTED, new String[]{encryptedChat.getId() + ""}, null, null, null);
                    while (cursor.moveToNext()) {
                        OldMessage msg = new OldMessage();
                        msg.setDatabaseId(cursor.getInt(cursor.getColumnIndex("_id")));
                        msg.setMid(cursor.getInt(cursor.getColumnIndex("mid")));
                        msg.setPeerType(cursor.getInt(cursor.getColumnIndex("peerType")));
                        msg.setPeerId(cursor.getInt(cursor.getColumnIndex("peerId")));
                        msg.setOut(cursor.getInt(cursor.getColumnIndex("isOut")) > 0);
                        msg.setState(cursor.getInt(cursor.getColumnIndex("state")));
                        msg.setDate(cursor.getInt(cursor.getColumnIndex("date")));
                        msg.setRandomId(cursor.getLong(cursor.getColumnIndex("randomId")));
                        msg.setForwardDate(cursor.getInt(cursor.getColumnIndex("forwardDate")));
                        msg.setForwardSenderId(cursor.getInt(cursor.getColumnIndex("forwardSenderId")));
                        msg.setForwardMid(cursor.getInt(cursor.getColumnIndex("forwardMid")));
                        msg.setContentType(cursor.getInt(cursor.getColumnIndex("contentType")));
                        msg.setSenderId(cursor.getInt(cursor.getColumnIndex("senderId")));
                        msg.setMessage(cursor.getString(cursor.getColumnIndex("message")));
                        msg.setDeletedLocal(cursor.getInt(cursor.getColumnIndex("deletedLocal")) > 0);
                        msg.setDeletedServer(cursor.getInt(cursor.getColumnIndex("deletedServer")) > 0);
                        msg.setExtras(cursor.getBlob(cursor.getColumnIndex("extras")));
                        msg.setMessageTimeout(cursor.getInt(cursor.getColumnIndex("messageTimeout")));
                        msg.setMessageDieTime(cursor.getInt(cursor.getColumnIndex("messageDieTime")));
                        oldMessages.add(msg);
                    }
                    cursor.close();
                }


                ArrayList<User> converted = new ArrayList<User>();
                for (OldUser user : users) {
                    User nu = new User();
                    nu.setUid(user.getUid());
                    nu.setFirstName(user.getFirstName());
                    nu.setLastName(user.getLastName());
                    nu.setPhone(user.getPhone());
                    nu.setAccessHash(user.getAccessHash());
                    nu.setLinkType(user.getLinkType());
                    try {
                        nu.setStatus((TLAbsLocalUserStatus) TLLocalContext.getInstance().deserializeMessage(user.getStatus()));
                    } catch (Exception e) {
                        nu.setStatus(new TLLocalUserStatusEmpty());
                    }

                    try {
                        nu.setPhoto((TLAbsLocalAvatarPhoto) TLLocalContext.getInstance().deserializeMessage(user.getStatus()));
                    } catch (Exception e) {
                        nu.setPhoto(new TLLocalAvatarEmpty());
                    }

                    converted.add(nu);
                }
                storageKernel.getModel().getUsersEngine().onUsersUncached(converted.toArray(new User[0]));

                ArrayList<Group> groups = new ArrayList<Group>();
                for (OldDialog dialog : dialogs) {
                    if (dialog.getPeerType() == PeerType.PEER_CHAT) {
                        Group g = new Group();
                        g.setTitle(dialog.getTitle());
                        g.setForbidden(dialog.getParticipantsCount() == 0);
                        g.setUsersCount(dialog.getParticipantsCount());
                        try {
                            if (dialog.getPhoto() != null) {
                                g.setAvatar((TLAbsLocalAvatarPhoto) TLLocalContext.getInstance().deserializeMessage(dialog.getPhoto()));
                            } else {
                                g.setAvatar(new TLLocalAvatarEmpty());
                            }
                        } catch (Exception e) {
                            g.setAvatar(new TLLocalAvatarEmpty());
                        }
                        g.setChatId(dialog.getPeerId());
                        groups.add(g);
                    }
                }
                storageKernel.getModel().getGroupsEngine().onGroupsUpdated(groups.toArray(new Group[groups.size()]));

                ArrayList<EncryptedChat> cEncChats = new ArrayList<EncryptedChat>();
                for (OldEncryptedChat encryptedChat : encryptedChats) {
                    EncryptedChat chat = new EncryptedChat();
                    chat.setId(encryptedChat.getId());
                    chat.setUserId(encryptedChat.getUid());
                    chat.setAccessHash(encryptedChat.getAccessHash());
                    chat.setState(encryptedChat.getState());
                    chat.setKey(encryptedChat.getKey());
                    chat.setSelfDestructTime(encryptedChat.getSelfDestruct());
                    chat.setOut(encryptedChat.isOut());
                    cEncChats.add(chat);
                }
                storageKernel.getModel().getSecretEngine().saveSecretChats(cEncChats.toArray(new EncryptedChat[0]));

                ArrayList<DialogDescription> cDialogs = new ArrayList<DialogDescription>();
                for (OldDialog dialog : dialogs) {
                    DialogDescription description = new DialogDescription();
                    description.setPeerType(dialog.getPeerType());
                    description.setPeerId(dialog.getPeerId());
                    description.setLastLocalViewedMessage(dialog.getLastLocalViewedMessage());
                    description.setLastRemoteViewedMessage(dialog.getLastRemoteViewedMessage());
                    description.setDate(dialog.getDate());
                    description.setContentType(dialog.getContentId());
                    description.setFailure(dialog.isFailureFlag());
                    description.setMessage(dialog.getMessage());
                    description.setMessageState(dialog.getMessageState());
                    description.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
                    description.setSenderId(dialog.getSenderId());
                    try {

                        if (dialog.getExtras() != null) {
                            description.setExtras(TLLocalContext.getInstance().deserializeMessage(dialog.getExtras()));
                        } else {
                            description.setExtras(new TLLocalEmpty());
                        }
                    } catch (Exception e) {
                        description.setExtras(new TLLocalEmpty());
                    }
                    description.setUnreadCount(dialog.getUnreadCount());
                    cDialogs.add(description);
                }

                storageKernel.getModel().getDialogsEngine().updateOrCreateDialog(cDialogs.toArray(new DialogDescription[0]));

                ArrayList<ChatMessage> cMessages = new ArrayList<ChatMessage>();
                for (OldMessage old : oldMessages) {
                    ChatMessage msg = new ChatMessage();
                    msg.setDatabaseId(old.getDatabaseId());
                    msg.setMid(old.getMid());
                    msg.setPeerId(old.getPeerId());
                    msg.setPeerType(old.getPeerType());
                    msg.setOut(old.isOut());
                    msg.setDate(old.getDate());
                    msg.setState(old.getState());
                    msg.setRandomId(old.getRandomId());
                    msg.setForwardDate(old.getForwardDate());
                    msg.setForwardSenderId(old.getForwardSenderId());
                    msg.setForwardMid(old.getForwardMid());
                    msg.setSenderId(old.getSenderId());
                    msg.setMessage(old.getMessage());
                    msg.setContentType(old.getContentType());
                    msg.setDeletedLocal(old.isDeletedLocal());
                    msg.setDeletedServer(old.isDeletedServer());
                    try {
                        if (old.getExtras() != null) {
                            msg.setExtras(TLLocalContext.getInstance().deserializeMessage(old.getExtras()));
                        } else {
                            msg.setExtras(new TLLocalEmpty());
                        }
                    } catch (Exception e) {
                        msg.setExtras(new TLLocalEmpty());
                    }
                    msg.setMessageDieTime(old.getMessageDieTime());
                    msg.setMessageTimeout(old.getMessageTimeout());
                    cMessages.add(msg);
                }
                storageKernel.getModel().getMessagesEngine().saveMessages(cMessages.toArray(new ChatMessage[0]));
            } catch (Exception e) {
                e.printStackTrace();
                storageKernel.getModel().dropData();
            } finally {
                try {
                    if (database != null) {
                        database.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!databasePath.delete()) {
                    databasePath.deleteOnExit();
                }
            }
        }
    }

    private static class OldMessage {
        private int databaseId;
        private int mid;
        private int peerType;
        private int peerId;
        private boolean isOut;
        private int state;
        private int date;
        private long randomId;
        private int forwardDate;
        private int forwardSenderId;
        private int forwardMid;
        private int contentType;
        private int senderId;
        private String message;
        private boolean deletedLocal;
        private boolean deletedServer;
        private byte[] extras;
        private int messageTimeout;
        private int messageDieTime;

        public int getDatabaseId() {
            return databaseId;
        }

        public void setDatabaseId(int databaseId) {
            this.databaseId = databaseId;
        }

        public int getMid() {
            return mid;
        }

        public void setMid(int mid) {
            this.mid = mid;
        }

        public int getPeerType() {
            return peerType;
        }

        public void setPeerType(int peerType) {
            this.peerType = peerType;
        }

        public int getPeerId() {
            return peerId;
        }

        public void setPeerId(int peerId) {
            this.peerId = peerId;
        }

        public boolean isOut() {
            return isOut;
        }

        public void setOut(boolean isOut) {
            this.isOut = isOut;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public int getDate() {
            return date;
        }

        public void setDate(int date) {
            this.date = date;
        }

        public long getRandomId() {
            return randomId;
        }

        public void setRandomId(long randomId) {
            this.randomId = randomId;
        }

        public int getForwardDate() {
            return forwardDate;
        }

        public void setForwardDate(int forwardDate) {
            this.forwardDate = forwardDate;
        }

        public int getForwardSenderId() {
            return forwardSenderId;
        }

        public void setForwardSenderId(int forwardSenderId) {
            this.forwardSenderId = forwardSenderId;
        }

        public int getForwardMid() {
            return forwardMid;
        }

        public void setForwardMid(int forwardMid) {
            this.forwardMid = forwardMid;
        }

        public int getContentType() {
            return contentType;
        }

        public void setContentType(int contentType) {
            this.contentType = contentType;
        }

        public int getSenderId() {
            return senderId;
        }

        public void setSenderId(int senderId) {
            this.senderId = senderId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public byte[] getExtras() {
            return extras;
        }

        public void setExtras(byte[] extras) {
            this.extras = extras;
        }

        public int getMessageTimeout() {
            return messageTimeout;
        }

        public void setMessageTimeout(int messageTimeout) {
            this.messageTimeout = messageTimeout;
        }

        public int getMessageDieTime() {
            return messageDieTime;
        }

        public void setMessageDieTime(int messageDieTime) {
            this.messageDieTime = messageDieTime;
        }

        public boolean isDeletedLocal() {
            return deletedLocal;
        }

        public void setDeletedLocal(boolean deletedLocal) {
            this.deletedLocal = deletedLocal;
        }

        public boolean isDeletedServer() {
            return deletedServer;
        }

        public void setDeletedServer(boolean deletedServer) {
            this.deletedServer = deletedServer;
        }
    }

    private static class OldEncryptedChat {
        private int id;
        private long accessHash;
        private int uid;
        private int state;
        private byte[] key;
        private int selfDestruct;
        private boolean isOut;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getAccessHash() {
            return accessHash;
        }

        public void setAccessHash(long accessHash) {
            this.accessHash = accessHash;
        }

        public int getUid() {
            return uid;
        }

        public void setUid(int uid) {
            this.uid = uid;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }

        public int getSelfDestruct() {
            return selfDestruct;
        }

        public void setSelfDestruct(int selfDestruct) {
            this.selfDestruct = selfDestruct;
        }

        public boolean isOut() {
            return isOut;
        }

        public void setOut(boolean isOut) {
            this.isOut = isOut;
        }
    }

    private static class OldUser {
        private int uid;
        private String firstName;
        private String lastName;
        private long accessHash;
        private String phone;
        private byte[] photo;
        private byte[] status;
        private int linkType;

        public int getUid() {
            return uid;
        }

        public void setUid(int uid) {
            this.uid = uid;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public long getAccessHash() {
            return accessHash;
        }

        public void setAccessHash(long accessHash) {
            this.accessHash = accessHash;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public byte[] getPhoto() {
            return photo;
        }

        public void setPhoto(byte[] photo) {
            this.photo = photo;
        }

        public byte[] getStatus() {
            return status;
        }

        public void setStatus(byte[] status) {
            this.status = status;
        }

        public int getLinkType() {
            return linkType;
        }

        public void setLinkType(int linkType) {
            this.linkType = linkType;
        }
    }

    private static class OldDialog {
        private int peerType;
        private int peerId;
        private String title;
        private byte[] photo;
        private int unreadCount;
        private int participantsCount;
        private int date;
        private int topMessageId;
        private int contentId;
        private String message;
        private int messageState;
        private int senderId;
        private int lastLocalViewedMessage;
        private int lastRemoteViewedMessage;
        private boolean failureFlag;
        private long firstUnreadMessage;
        private byte[] extras;

        public int getSenderId() {
            return senderId;
        }

        public void setSenderId(int senderId) {
            this.senderId = senderId;
        }

        public int getPeerType() {
            return peerType;
        }

        public void setPeerType(int peerType) {
            this.peerType = peerType;
        }

        public int getPeerId() {
            return peerId;
        }

        public void setPeerId(int peerId) {
            this.peerId = peerId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public byte[] getPhoto() {
            return photo;
        }

        public void setPhoto(byte[] photo) {
            this.photo = photo;
        }

        public int getUnreadCount() {
            return unreadCount;
        }

        public void setUnreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
        }

        public int getParticipantsCount() {
            return participantsCount;
        }

        public void setParticipantsCount(int participantsCount) {
            this.participantsCount = participantsCount;
        }

        public int getDate() {
            return date;
        }

        public void setDate(int date) {
            this.date = date;
        }

        public int getTopMessageId() {
            return topMessageId;
        }

        public void setTopMessageId(int topMessageId) {
            this.topMessageId = topMessageId;
        }

        public int getContentId() {
            return contentId;
        }

        public void setContentId(int contentId) {
            this.contentId = contentId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getMessageState() {
            return messageState;
        }

        public void setMessageState(int messageState) {
            this.messageState = messageState;
        }

        public int getLastLocalViewedMessage() {
            return lastLocalViewedMessage;
        }

        public void setLastLocalViewedMessage(int lastLocalViewedMessage) {
            this.lastLocalViewedMessage = lastLocalViewedMessage;
        }

        public int getLastRemoteViewedMessage() {
            return lastRemoteViewedMessage;
        }

        public void setLastRemoteViewedMessage(int lastRemoteViewedMessage) {
            this.lastRemoteViewedMessage = lastRemoteViewedMessage;
        }

        public boolean isFailureFlag() {
            return failureFlag;
        }

        public void setFailureFlag(boolean failureFlag) {
            this.failureFlag = failureFlag;
        }

        public long getFirstUnreadMessage() {
            return firstUnreadMessage;
        }

        public void setFirstUnreadMessage(long firstUnreadMessage) {
            this.firstUnreadMessage = firstUnreadMessage;
        }

        public byte[] getExtras() {
            return extras;
        }

        public void setExtras(byte[] extras) {
            this.extras = extras;
        }
    }
}
