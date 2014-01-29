package org.telegram.android.core.contacts;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 27.01.14.
 */
public class SyncContact {
    private long contactId;
    private String firstName;
    private String lastName;

    private ArrayList<SyncPhone> syncPhones = new ArrayList<SyncPhone>();

    public SyncContact(long contactId, String firstName, String lastName) {
        this.contactId = contactId;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public long getContactId() {
        return contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public ArrayList<SyncPhone> getSyncPhones() {
        return syncPhones;
    }
}
