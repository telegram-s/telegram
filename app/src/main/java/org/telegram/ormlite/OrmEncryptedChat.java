package org.telegram.ormlite;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ex3ndr on 03.01.14.
 */
@DatabaseTable(tableName = "encryptedchat")
public class OrmEncryptedChat {
    @DatabaseField(generatedId = false, id = true, version = false)
    private int _id;

    @DatabaseField(version = false)
    private long accessHash;

    @DatabaseField(version = false)
    private int userId;

    @DatabaseField(version = false)
    private int state;

    @DatabaseField(version = false, dataType = DataType.BYTE_ARRAY)
    private byte[] key;

    @DatabaseField(version = false)
    private int selfDestructTime;

    @DatabaseField(version = false)
    private boolean isOut;

    public int getId() {
        return _id;
    }

    public void setId(int _id) {
        this._id = _id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
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

    public int getSelfDestructTime() {
        return selfDestructTime;
    }

    public void setSelfDestructTime(int selfDestructTime) {
        this.selfDestructTime = selfDestructTime;
    }

    public boolean isOut() {
        return isOut;
    }

    public void setOut(boolean isOut) {
        this.isOut = isOut;
    }
}
