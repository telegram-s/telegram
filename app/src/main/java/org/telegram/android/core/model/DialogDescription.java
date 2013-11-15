package org.telegram.android.core.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.Serializable;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:19
 */
public class DialogDescription implements Serializable {
    @DatabaseField(generatedId = true, version = false)
    private int _id;

    @DatabaseField(index = true, version = false)
    private int peerType;

    @DatabaseField(index = true, version = false)
    private int peerId;

    @DatabaseField(version = false)
    private String title;

    @DatabaseField(version = false, persisterClass = TlDataType.class)
    private TLAbsLocalAvatarPhoto photo;

    @DatabaseField(version = false)
    private int unreadCount;

    @DatabaseField(version = false)
    private int participantsCount;

    // Last message content

    @DatabaseField(version = false)
    private int date;

    @DatabaseField(version = false)
    private String senderTitle;

    @DatabaseField(version = false)
    private int senderId;

    @DatabaseField(version = false)
    private int topMessageId;

    @DatabaseField(version = false)
    private int contentType;

    @DatabaseField(version = false)
    private String message;

    @DatabaseField(version = false)
    private int messageState;

    @DatabaseField(version = false)
    private boolean messageHasSmileys;

    @DatabaseField(version = false)
    private int lastLocalViewedMessage;

    @DatabaseField(version = false)
    private int lastRemoteViewedMessage;

    @DatabaseField(version = false)
    private boolean failure;

    @DatabaseField(version = false, persisterClass = TlDataType.class)
    private TLObject extras;

    @DatabaseField(version = false)
    private int firstUnreadMessage;

    public int getDatabaseId() {
        return _id;
    }

    public void setDatabaseId(int _id) {
        this._id = _id;
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

    public TLAbsLocalAvatarPhoto getPhoto() {
        return photo;
    }

    public void setPhoto(TLAbsLocalAvatarPhoto photo) {
        this.photo = photo;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public String getSenderTitle() {
        return senderTitle;
    }

    public void setSenderTitle(String senderTitle) {
        this.senderTitle = senderTitle;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getRawContentType() {
        return contentType & ContentType.CONTENT_MASK;
    }

    public boolean isForwarded() {
        return (contentType & ContentType.MESSAGE_FORWARDED) != 0;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        messageHasSmileys = EmojiProcessor.containsEmoji(message);
    }

    public int getTopMessageId() {
        return topMessageId;
    }

    public void setTopMessageId(int topMessageId) {
        this.topMessageId = topMessageId;
    }

    public boolean isMessageHasSmileys() {
        return messageHasSmileys;
    }

    public void setMessageHasSmileys(boolean messageHasSmileys) {
        this.messageHasSmileys = messageHasSmileys;
    }

    public int getMessageState() {
        return messageState;
    }

    public void setMessageState(int messageState) {
        this.messageState = messageState;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
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

    public boolean isFailure() {
        return failure;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    public TLObject getExtras() {
        return extras;
    }

    public void setExtras(TLObject extras) {
        this.extras = extras;
    }

    public int getFirstUnreadMessage() {
        return firstUnreadMessage;
    }

    public void setFirstUnreadMessage(int firstUnreadMessage) {
        this.firstUnreadMessage = firstUnreadMessage;
    }
}
