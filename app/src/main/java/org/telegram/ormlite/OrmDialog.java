package org.telegram.ormlite;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.telegram.android.core.model.ContentType;
import org.telegram.android.core.model.TlDataType;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.tl.TLObject;

/**
 * Created by ex3ndr on 02.01.14.
 */
@DatabaseTable(tableName = "dialogdescription")
public class OrmDialog {
    @DatabaseField(id = true, version = false)
    private long _id;

    @DatabaseField(version = false)
    private String title;

    @DatabaseField(version = false)
    private int unreadCount;

    @DatabaseField(version = false)
    private int participantsCount;

    // Last message content

    @DatabaseField(version = false, index = true)
    private int date;

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

//    @DatabaseField(version = false, persisterClass = TlDataType.class)
//    private TLObject extras;

    @DatabaseField(version = false)
    private long firstUnreadMessage;

    public long getUniqId() {
        return _id;
    }

    public void setPeer(int peerType, int peerId) {
        this._id = peerId * 10L + peerType;
    }

    public int getPeerType() {
        return (int) (_id % 10L);
    }

    public int getPeerId() {
        return (int) (_id / 10L);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

//    public TLObject getExtras() {
//        return extras;
//    }
//
//    public void setExtras(TLObject extras) {
//        this.extras = extras;
//    }

    public long getFirstUnreadMessage() {
        return firstUnreadMessage;
    }

    public void setFirstUnreadMessage(long firstUnreadMessage) {
        this.firstUnreadMessage = firstUnreadMessage;
    }
}
