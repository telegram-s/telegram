package org.telegram.android.core.model;

import org.telegram.tl.TLObject;

import java.io.Serializable;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:28
 */
public class ChatMessage implements Serializable {
    private int _id;
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
    private TLObject extras;

    private boolean deletedLocal;
    private boolean deletedServer;
    private int messageTimeout;
    private int messageDieTime;

    public int getDatabaseId() {
        return _id;
    }

    public void setDatabaseId(int _id) {
        this._id = _id;
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

    public void setOut(boolean out) {
        isOut = out;
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

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getRawContentType() {
        return contentType & ContentType.CONTENT_MASK;
    }

    public boolean isForwarded() {
        return (contentType & ContentType.MESSAGE_FORWARDED) != 0;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TLObject getExtras() {
        return extras;
    }

    public void setExtras(TLObject extras) {
        this.extras = extras;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
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

    public long getRandomId() {
        return randomId;
    }

    public void setRandomId(long randomId) {
        this.randomId = randomId;
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
}