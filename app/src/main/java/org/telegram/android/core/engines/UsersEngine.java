package org.telegram.android.core.engines;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.*;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLAbsUserStatus;
import org.telegram.api.TLContact;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class UsersEngine {

    private static final String TAG = "UsersEngine";

    private RuntimeExceptionDao<Contact, Long> contactsDao;

    private StelsApplication application;

    private boolean isContactsLoaded = false;
    private final Object contactsCacheLock = new Object();
    private final CopyOnWriteArrayList<Contact> cachedContacts = new CopyOnWriteArrayList<Contact>();

    private UsersDatabase usersDatabase;


    public UsersEngine(ModelEngine modelEngine) {
        this.application = modelEngine.getApplication();
        this.contactsDao = modelEngine.getContactsDao();

        this.usersDatabase = new UsersDatabase(application, modelEngine);
    }

    public void clearCache() {
        this.usersDatabase.clearCache();
        synchronized (contactsCacheLock) {
            isContactsLoaded = false;
            cachedContacts.clear();
        }
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

    private void checkContacts() {
        if (isContactsLoaded) {
            return;
        }
        synchronized (contactsCacheLock) {
            Contact[] contacts = contactsDao.queryForAll().toArray(new Contact[0]);
            cachedContacts.clear();
            Collections.addAll(cachedContacts, contacts);
            isContactsLoaded = true;
        }
    }

    public User[] getContacts() {
        return usersDatabase.getContacts();
    }

    public void onImportedContacts(final HashMap<Long, HashSet<Integer>> importedContacts) {
        contactsDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                HashSet<Integer> allUids = new HashSet<Integer>();
                for (Long localId : importedContacts.keySet()) {
                    Contact[] contacts = getContactsForLocalId(localId);
                    HashSet<Integer> uids = importedContacts.get(localId);
                    allUids.addAll(uids);
                    for (Integer uid : uids) {
                        boolean contains = false;
                        for (Contact contact : contacts) {
                            if (contact.getUid() == uid) {
                                contains = true;
                                break;
                            }
                        }

                        if (!contains) {
                            Contact contact = new Contact(uid, localId, false);
                            cachedContacts.add(contact);
                            contactsDao.create(contact);
                        }
                    }

                    for (Contact contact : contacts) {
                        boolean contains = false;
                        for (Integer uid : uids) {
                            if (contact.getUid() == uid) {
                                contains = true;
                                break;
                            }
                        }

                        if (!contains) {
                            cachedContacts.remove(contact);
                            contactsDao.delete(contact);
                        }
                    }
                }

                ArrayList<User> updated = new ArrayList<User>();
                for (User u : getUsersById(allUids.toArray())) {
                    if (u.getLinkType() != LinkType.CONTACT) {
                        u.setLinkType(LinkType.CONTACT);
                        updated.add(u);
                        forceUpdateUser(u);
                    }
                }
                return null;
            }
        });
    }

    public Contact[] getAllContacts() {
        checkContacts();
        return cachedContacts.toArray(new Contact[0]);
    }

    public Contact[] getContactsForLocalId(long localId) {
        checkContacts();

        synchronized (contactsCacheLock) {
            ArrayList<Contact> contacts = new ArrayList<Contact>();
            for (Contact cached : cachedContacts) {
                if (cached.getLocalId() == localId) {
                    contacts.add(cached);
                }
            }

            return contacts.toArray(new Contact[contacts.size()]);
        }
    }

    public int[] getUidsForLocalId(long localId) {
        checkContacts();

        synchronized (contactsCacheLock) {
            ArrayList<Contact> contacts = new ArrayList<Contact>();
            for (Contact cached : cachedContacts) {
                if (cached.getLocalId() == localId) {
                    contacts.add(cached);
                }
            }

            int[] res = new int[contacts.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = contacts.get(i).getUid();
            }
            return res;
        }
    }

    public long[] getContactsForUid(int uid) {
        checkContacts();

        synchronized (contactsCacheLock) {
            ArrayList<Contact> contacts = new ArrayList<Contact>();
            for (Contact cached : cachedContacts) {
                if (cached.getUid() == uid) {
                    contacts.add(cached);
                }
            }

            long[] res = new long[contacts.size()];
            for (int i = 0; i < res.length; i++) {
                res[i] = contacts.get(i).getLocalId();
            }
            return res;
        }
    }

    public void deleteContactsForLocalId(long localId) {
        try {
            DeleteBuilder<Contact, Long> builder = contactsDao.deleteBuilder();
            builder.where().eq("localId", localId);
            contactsDao.delete(builder.prepare());
            for (Contact cached : cachedContacts) {
                if (cached.getLocalId() == localId) {
                    cachedContacts.remove(cached);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void onContacts(List<TLAbsUser> users, List<TLContact> contacts) {
        onUsers(users);
    }
}
