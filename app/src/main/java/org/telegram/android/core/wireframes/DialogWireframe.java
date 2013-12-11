package org.telegram.android.core.wireframes;

import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.service.TLAbsLocalAction;
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class DialogWireframe {

    private Object preparedLayout;

    private int databaseId;

    private int peerId;
    private int peerType;
    private User dialogUser;

    private TLAbsLocalAvatarPhoto chatAvatar;
    private String chatTitle;

    private boolean isOut;
    private boolean isMine;
    private int senderId;
    private int messageState;
    private User sender;
    private int date;
    private int contentType;
    private String message;
    private TLAbsLocalAction localAction;

    private boolean isErrorState;
    private int unreadCount;

    public DialogWireframe(int databaseId, int peerId, int peerType) {
        this.peerId = peerId;
        this.peerType = peerType;
        this.databaseId = databaseId;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public int getPeerId() {
        return peerId;
    }

    public int getPeerType() {
        return peerType;
    }

    public String getDialogTitle() {
        if (peerType == PeerType.PEER_USER && peerId == 333000) {
            return "Telegram";
        } else if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
            if (dialogUser.getLinkType() == LinkType.REQUEST) {
                return TextUtil.formatPhone(dialogUser.getPhone());
            } else {
                return dialogUser.getDisplayName();
            }
        } else if (peerType == PeerType.PEER_CHAT) {
            return chatTitle;
        } else {
            throw new RuntimeException("Unknown peer type: " + peerType);
        }
    }

    public TLAbsLocalAvatarPhoto getDialogAvatar() {
        if (peerType == PeerType.PEER_USER || peerType == PeerType.PEER_USER_ENCRYPTED) {
            return dialogUser.getPhoto();
        } else {
            return chatAvatar;
        }
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean isMine) {
        this.isMine = isMine;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }

    public boolean isErrorState() {
        return isErrorState;
    }

    public TLAbsLocalAction getLocalAction() {
        return localAction;
    }

    public void setLocalAction(TLAbsLocalAction localAction) {
        this.localAction = localAction;
    }

    public void setErrorState(boolean isErrorState) {
        this.isErrorState = isErrorState;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public User getDialogUser() {
        return dialogUser;
    }

    public void setDialogUser(User dialogUser) {
        this.dialogUser = dialogUser;
    }

    public TLAbsLocalAvatarPhoto getChatAvatar() {
        return chatAvatar;
    }

    public void setChatAvatar(TLAbsLocalAvatarPhoto chatAvatar) {
        this.chatAvatar = chatAvatar;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean isOut) {
        this.isOut = isOut;
    }

    public int getMessageState() {
        return messageState;
    }

    public void setMessageState(int messageState) {
        this.messageState = messageState;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getPreparedLayout() {
        return preparedLayout;
    }

    public void setPreparedLayout(Object preparedLayout) {
        this.preparedLayout = preparedLayout;
    }
}