package org.telegram.android.core.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.api.TLAbsUserStatus;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 21:20
 */

public class User {
    @DatabaseField(generatedId = true, version = false)
    private int _id;

    @DatabaseField(index = true, version = false)
    private int uid;

    @DatabaseField(index = true, version = false)
    private String firstName;

    @DatabaseField(index = true, version = false)
    private String lastName;

    @DatabaseField(index = true, version = false)
    private long accessHash;

    @DatabaseField(index = true, version = false)
    private String phone;

    @DatabaseField(index = true, version = false, persisterClass = TlDataType.class)
    private TLAbsLocalAvatarPhoto photo;

    @DatabaseField(index = true, version = false, persisterClass = TlDataType.class)
    private TLAbsLocalUserStatus status;

    @DatabaseField(index = true, version = false)
    private int linkType;

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

    public String getFirstName() {
        if (uid == 333000) {
            return "Telegram";
        }
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        if (uid == 333000) {
            return "";
        }
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        if (uid == 333000) {
            return "Telegram";
        }
        if (lastName == null || lastName.length() == 0) {
            return firstName;
        } else {
            return firstName + " " + lastName;
        }
    }

    public String getSortingName() {
        if (uid == 333000) {
            return "Telegram";
        }
        if (lastName == null || lastName.length() == 0) {
            if (firstName.length() == 0) {
                return "#";
            }
            return firstName;
        } else {
            if (lastName.length() == 0) {
                return "#";
            }
            return lastName;
        }
    }

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public TLAbsLocalAvatarPhoto getPhoto() {
        return photo;
    }

    public void setPhoto(TLAbsLocalAvatarPhoto photo) {
        this.photo = photo;
    }

    public TLAbsLocalUserStatus getStatus() {
        return status;
    }

    public void setStatus(TLAbsLocalUserStatus status) {
        this.status = status;
    }

    public int getLinkType() {
        return linkType;
    }

    public void setLinkType(int linkType) {
        this.linkType = linkType;
    }
}
