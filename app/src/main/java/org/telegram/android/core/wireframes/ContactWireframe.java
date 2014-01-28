package org.telegram.android.core.wireframes;

import org.telegram.android.core.model.User;

/**
 * Created by ex3ndr on 28.01.14.
 */
public class ContactWireframe {
    private long contactId;
    private String lookupKey;

    private String displayName;
    private String header;
    private char section;
    private User[] relatedUsers;

    public ContactWireframe(long contactId, String lookupKey, String displayName, String header) {
        this.contactId = contactId;
        this.lookupKey = lookupKey;
        this.displayName = displayName;
        this.relatedUsers = new User[0];
        this.header = header;
        this.section = '?';
    }

    public ContactWireframe(long contactId, String lookupKey, String displayName) {
        this.contactId = contactId;
        this.lookupKey = lookupKey;
        this.displayName = displayName;
        this.relatedUsers = new User[0];
        setHeader(displayName);
    }

    public ContactWireframe(User user, boolean sortByFirstName) {
        this.displayName = user.getDisplayName();
        this.relatedUsers = new User[]{user};
        if (sortByFirstName) {
            setHeader(user.getFirstName());
        } else {
            setHeader(user.getLastName());
        }
    }

    public long getContactId() {
        return contactId;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHeader() {
        return header;
    }

    public char getSection() {
        return section;
    }

    public User[] getRelatedUsers() {
        return relatedUsers;
    }

    private void setHeader(String s) {
        if (s == null || s.length() == 0) {
            header = "#";
        } else if (Character.isLetter(s.charAt(0))) {
            header = "" + (s.charAt(0) + "").toUpperCase().charAt(0);
        } else {
            header = "#";
        }
        section = header.charAt(0);
    }
}
