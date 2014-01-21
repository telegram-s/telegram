package org.telegram.android.core.engines;

import android.os.SystemClock;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLAbsUserStatus;
import org.telegram.api.TLContact;

import java.util.*;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class UsersEngine {

    private static final String TAG = "UsersEngine";

    private TelegramApplication application;

    private UsersDatabase usersDatabase;
    private ContactsDatabase contactsDatabase;

    public UsersEngine(ModelEngine modelEngine) {
        this.application = modelEngine.getApplication();
        this.usersDatabase = new UsersDatabase(application, modelEngine);
        this.contactsDatabase = new ContactsDatabase(modelEngine);
    }

    public User getUserRuntime(int id) {
        User res = getUser(id);

        if (res == null) {
            throw new RuntimeException("Unknown user #" + id);
        }

        return res;
    }

    public User getUser(int id) {
        return usersDatabase.getUser(id);
    }

    public User[] getUsersById(Object[] uid) {
        return usersDatabase.getUsersById(uid);
    }

    public void onUserLinkChanged(int uid, int link) {
        User user = getUser(uid);
        if (user == null) {
            return;
        }
        user.setLinkType(link);
        forceUpdateUser(user);
    }

    public void onUserNameChanges(int uid, String firstName, String lastName) {
        User u = getUser(uid);
        if (u != null) {
            u.setFirstName(firstName);
            u.setLastName(lastName);
            forceUpdateUser(u);
        }
    }

    public void onUserPhotoChanges(int uid, TLAbsLocalAvatarPhoto photo) {
        User u = getUser(uid);
        if (u != null) {
            u.setPhoto(photo);
            forceUpdateUser(u);
        }
    }

    public void onUserStatus(int uid, TLAbsUserStatus status) {
        User u = getUser(uid);
        if (u != null) {
            u.setStatus(EngineUtils.convertStatus(status));
            forceUpdateUser(u);
        }
    }

    private void forceUpdateUser(User u) {
        Logger.d(TAG, "forceUpdateUser");
        long start = SystemClock.uptimeMillis();
        usersDatabase.updateUsers(u);
        Logger.d(TAG, "forceUpdateUser in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void onUsersUncached(User... converted) {
        onUsers(converted);
    }

    private void onUsers(User... converted) {
        usersDatabase.updateUsers(converted);
    }

    public void onUsers(List<TLAbsUser> users) {
        User[] converted = new User[users.size()];
        for (int i = 0; i < users.size(); i++) {
            converted[i] = EngineUtils.userFromTlUser(users.get(i));
        }

        onUsers(converted);
    }

    // Contacts

    public User[] getContacts() {
        return usersDatabase.getContacts();
    }

    public void onImportedContacts(final HashMap<Long, HashSet<Integer>> importedContacts) {
        ArrayList<Contact> nContacts = new ArrayList<Contact>();
        HashSet<Integer> allUids = new HashSet<Integer>();
        for (Long localId : importedContacts.keySet()) {
            HashSet<Integer> uids = importedContacts.get(localId);
            allUids.addAll(uids);
            for (Integer uid : uids) {
                nContacts.add(new Contact(uid, localId));
            }
        }
        if (nContacts.size() > 0) {
            contactsDatabase.writeContacts(nContacts.toArray(new Contact[0]));
        }

        ArrayList<User> updated = new ArrayList<User>();
        for (User u : getUsersById(allUids.toArray())) {
            if (u.getLinkType() != LinkType.CONTACT) {
                u.setLinkType(LinkType.CONTACT);
                updated.add(u);
            }
        }
        if (updated.size() > 0) {
            usersDatabase.updateUsers(updated.toArray(new User[0]));
        }
    }

    public Contact[] getAllContacts() {
        return contactsDatabase.readAllContacts();
    }

    public Contact[] getContactsForLocalId(long localId) {
        return contactsDatabase.getContactsForLocalId(localId);
    }

    public int[] getUidsForLocalId(long localId) {
        return contactsDatabase.getUidsForLocalId(localId);
    }

    public long[] getContactsForUid(int uid) {
        return contactsDatabase.getContactsForUid(uid);
    }

    public void deleteContactsForLocalId(long localId) {
        contactsDatabase.deleteContactsForLocalId(localId);
    }

    public void onContacts(List<TLAbsUser> users, List<TLContact> contacts) {
        onUsers(users);
    }

    public void clear() {
        contactsDatabase.clear();
        usersDatabase.clear();
    }
}
