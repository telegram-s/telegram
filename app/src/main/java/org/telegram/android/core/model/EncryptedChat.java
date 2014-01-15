package org.telegram.android.core.model;


/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 08.10.13
 * Time: 18:36
 */
public class EncryptedChat {
    private int _id;
    private long accessHash;
    private int userId;
    private int state;
    private byte[] key;
    private int selfDestructTime;
    private boolean isOut;

    public EncryptedChat(int _id, int userId, int state) {
        this._id = _id;
        this.userId = userId;
        this.state = state;
    }

    public EncryptedChat() {

    }

    public int getId() {
        return _id;
    }

    public void setId(int _id) {
        this._id = _id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
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

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
    }

    public int getSelfDestructTime() {
        return selfDestructTime;
    }

    public void setSelfDestructTime(int selfDestructTime) {
        this.selfDestructTime = selfDestructTime;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean out) {
        isOut = out;
    }
}
