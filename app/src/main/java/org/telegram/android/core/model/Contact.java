package org.telegram.android.core.model;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 4:24
 */
public class Contact {
    private int uid;
    private long localId;

    public Contact() {

    }

    public Contact(int uid, long localId) {
        this.uid = uid;
        this.localId = localId;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
    }
}
