package org.telegram.android.core.model;

import com.j256.ormlite.field.DatabaseField;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 4:24
 */
public class Contact {
    @DatabaseField(generatedId = true, version = false)
    private int _id;

    @DatabaseField(index = true, version = false)
    private int uid;

    @DatabaseField(index = true, version = false)
    private long localId;

    @DatabaseField(version = false, foreign = true, foreignAutoRefresh = false)
    private User user;

    @DatabaseField(version = false)
    private boolean isMutual;

    public Contact() {

    }

    public Contact(int uid, long localId, User user, boolean mutual) {
        this.uid = uid;
        this.user = user;
        this.localId = localId;
        isMutual = mutual;
    }

    public int getDatabaseId() {
        return _id;
    }

    public void setDatabaseId(int _id) {
        this._id = _id;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public boolean isMutual() {
        return isMutual;
    }

    public void setMutual(boolean mutual) {
        isMutual = mutual;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }
}
