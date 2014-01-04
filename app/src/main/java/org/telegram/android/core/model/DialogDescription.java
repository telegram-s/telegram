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
    private int peerType;
    private int peerId;
    private int unreadCount;
    private int date;
    private int senderId;
    private int topMessageId;
    private int contentType;
    private String message;
    private int messageState;
    private int lastLocalViewedMessage;
    private int lastRemoteViewedMessage;
    private boolean failure;
    private TLObject extras;
    private long firstUnreadMessage;

    public int getUniqId() {
        return peerId * 10 + peerType;
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
    }

    public int getTopMessageId() {
        return topMessageId;
    }

    public void setTopMessageId(int topMessageId) {
        this.topMessageId = topMessageId;
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

    public long getFirstUnreadMessage() {
        return firstUnreadMessage;
    }

    public void setFirstUnreadMessage(long firstUnreadMessage) {
        this.firstUnreadMessage = firstUnreadMessage;
    }
}
