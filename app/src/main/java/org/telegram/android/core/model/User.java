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
    private int uid;
    private String firstName;
    private String lastName;
    private long accessHash;
    private String phone;
    private TLAbsLocalAvatarPhoto photo;
    private TLAbsLocalUserStatus status;
    private int linkType;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) {
            return false;
        }

        return equals((User) o);
    }

    public boolean equals(User u) {
        if (uid != u.uid) {
            return false;
        }

        if (accessHash != u.accessHash) {
            return false;
        }

        if (linkType != u.linkType) {
            return false;
        }

        if (firstName != null && u.firstName != null) {
            if (!firstName.equals(u.firstName)) {
                return false;
            }
        } else {
            // Both or one is null
            if (firstName != u.firstName) {
                return false;
            }
        }

        if (lastName != null && u.lastName != null) {
            if (!lastName.equals(u.lastName)) {
                return false;
            }
        } else {
            // Both or one is null
            if (lastName != u.lastName) {
                return false;
            }
        }

        if (phone != null && u.phone != null) {
            if (!phone.equals(u.phone)) {
                return false;
            }
        } else {
            // Both or one is null
            if (phone != u.phone) {
                return false;
            }
        }

        if (photo != null && u.photo != null) {
            if (!photo.equals(u.photo)) {
                return false;
            }
        } else {
            // Both or one is null
            if (photo != u.photo) {
                return false;
            }
        }

        if (status != null && u.status != null) {
            if (!status.equals(u.status)) {
                return false;
            }
        } else {
            // Both or one is null
            if (status != u.status) {
                return false;
            }
        }

        return true;
    }


}
