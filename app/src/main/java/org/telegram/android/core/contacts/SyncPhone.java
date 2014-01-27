package org.telegram.android.core.contacts;

/**
 * Created by ex3ndr on 27.01.14.
 */
public class SyncPhone {
    private long phoneId;
    private String number;
    private SyncContact contact;
    private boolean isPrimary;
    private boolean isImported;

    public SyncPhone(long phoneId, String number, SyncContact contact) {
        this.phoneId = phoneId;
        this.number = number;
        this.contact = contact;
    }

    public SyncContact getContact() {
        return contact;
    }

    public long getPhoneId() {
        return phoneId;
    }

    public String getNumber() {
        return number;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public boolean isImported() {
        return isImported;
    }

    public void setImported(boolean isImported) {
        this.isImported = isImported;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
