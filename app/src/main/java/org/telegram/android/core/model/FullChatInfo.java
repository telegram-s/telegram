package org.telegram.android.core.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 11:48
 */
public class FullChatInfo {
    @DatabaseField(id = true, version = false)
    private int _id;

    @DatabaseField(version = false)
    private int adminId;

    @DatabaseField(version = false, dataType = DataType.SERIALIZABLE)
    private int[] uids;

    @DatabaseField(version = false, dataType = DataType.SERIALIZABLE)
    private int[] inviters;

    @DatabaseField(version = false)
    private int version;

    @DatabaseField(version = false)
    private boolean isForbidden;

    public int getChatId() {
        return _id;
    }

    public void setChatId(int _id) {
        this._id = _id;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public int[] getUids() {
        return uids;
    }

    public void setUids(int[] uids) {
        this.uids = uids;
    }

    public int[] getInviters() {
        return inviters;
    }

    public void setInviters(int[] inviters) {
        this.inviters = inviters;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isForbidden() {
        return isForbidden;
    }

    public void setForbidden(boolean forbidden) {
        isForbidden = forbidden;
    }
}
