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

    private ConcurrentHashMap<Integer, User> userCache;

    private RuntimeExceptionDao<User, Long> userDao;
    private RuntimeExceptionDao<Contact, Long> contactsDao;

    private PreparedQuery<User> getUserQuery;
    private SelectArg getUserIdArg;

    private StelsApplication application;

    private boolean isContactsLoaded = false;
    private final Object contactsCacheLock = new Object();
    private final CopyOnWriteArrayList<Contact> cachedContacts = new CopyOnWriteArrayList<Contact>();


    public UsersEngine(ModelEngine modelEngine) {
        this.application = modelEngine.getApplication();
        this.userCache = new ConcurrentHashMap<Integer, User>();
        this.userDao = modelEngine.getUsersDao();
        this.contactsDao = modelEngine.getContactsDao();

        try {
            QueryBuilder<User, Long> queryBuilder = userDao.queryBuilder();
            getUserIdArg = new SelectArg();
            queryBuilder.where().eq("uid", getUserIdArg);
            getUserQuery = queryBuilder.prepare();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clearCache() {
        userCache.clear();
        synchronized (contactsCacheLock) {
            isContactsLoaded = false;
            cachedContacts.clear();
        }
    }

    public User getUserRuntime(int id) {
        User res = userCache.get(id);
        if (res != null)
            return res;

        synchronized (getUserQuery) {
            getUserIdArg.setValue(id);
            List<User> users = userDao.query(getUserQuery);
            if (users.size() > 0) {
                userCache.putIfAbsent(id, users.get(0));
                return userCache.get(id);
            }
        }

        throw new RuntimeException("Unknown user #" + id);
    }

    public User getUser(int id) {
        User res = userCache.get(id);
        if (res != null)
            return res;

        synchronized (getUserQuery) {
            getUserIdArg.setValue(id);
            List<User> users = userDao.query(getUserQuery);
            if (users.size() > 0) {
                userCache.putIfAbsent(id, users.get(0));
                return userCache.get(id);
            }
        }

        return null;
    }

    public User[] getUsersById(Object[] uid) {
        try {
            ArrayList<Object> missedMids = new ArrayList<Object>();
            ArrayList<User> users = new ArrayList<User>();
            for (Object o : uid) {
                User u = userCache.get(o);
                if (u != null) {
                    users.add(u);
                } else {
                    missedMids.add(o);
                }
            }
            if (missedMids.size() > 0) {
                long start = System.currentTimeMillis();
                QueryBuilder<User, Long> queryBuilder = userDao.queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                User[] res = queryBuilder.query().toArray(new User[0]);
                Logger.d(TAG, "Loaded users in " + (System.currentTimeMillis() - start) + " ms");
                for (int i = 0; i < res.length; i++) {
                    users.add(cacheUser(res[i]));
                }
            }
            return users.toArray(new User[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new User[0];
    }

    public User cacheUser(User src) {
        userCache.putIfAbsent(src.getUid(), src);
        return userCache.get(src.getUid());
    }

    public void onUserLinkChanged(int uid, int link) {
        User user = getUser(uid);
        if (user == null) {
            return;
        }
        user.setLinkType(link);
        onUsers(user);
    }

    public void onUserNameChanges(int uid, String firstName, String lastName) {
        User u = getUser(uid);
        if (u != null) {
            u.setFirstName(firstName);
            u.setLastName(lastName);
            onUsers(u);
        }
    }

    public void onUserPhotoChanges(int uid, TLAbsLocalAvatarPhoto photo) {
        User u = getUser(uid);
        if (u != null) {
            u.setPhoto(photo);
            onUsers(u);
        }
    }

    public void onUserStatus(int uid, TLAbsUserStatus status) {
        User u = getUser(uid);
        if (u != null) {
            u.setStatus(EngineUtils.convertStatus(status));
            onUsers(u);
        }
    }

    public void onUsers(User... converted) {
        long start = SystemClock.uptimeMillis();
        Logger.d(TAG, "onUsers: " + converted.length);

        Integer[] ids = new Integer[converted.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = converted[i].getUid();
        }
        User[] original = getUsersById(ids);

        Logger.d(TAG, "onUsers original in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();

        final ArrayList<Pair<User, User>> diff = new ArrayList<Pair<User, User>>();

        int changed = 0;

        for (User m : converted) {
            User orig = EngineUtils.searchUser(original, m.getUid());

            boolean isChanged = false;

            if (orig != null) {
                if (!orig.equals(m)) {
                    isChanged = true;
                    changed++;
                }
            } else {
                isChanged = true;
                changed++;
            }

            if (isChanged) {
                diff.add(new Pair<User, User>(orig, m));
            }

        }

        Logger.d(TAG, "onUsers changed " + changed + " of " + converted.length);

        if (changed == 0) {
            return;
        }

        Logger.d(TAG, "onUsers prepared in " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        userDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                int changedColumns = 0;
                for (Pair<User, User> user : diff) {
                    User orig = user.first;
                    User changed = user.second;

                    if (orig == null) {
                        userDao.create(changed);
                        userCache.putIfAbsent(changed.getUid(), changed);
                    } else {
                        UpdateBuilder<User, Long> updateBuilder = userDao.updateBuilder();
                        updateBuilder.where().eq("_id", orig.getDatabaseId());

                        if (orig.getAccessHash() != changed.getAccessHash()) {
                            updateBuilder.updateColumnValue("accessHash", changed.getAccessHash());
                            orig.setAccessHash(changed.getAccessHash());
                            changedColumns++;
                        }

                        if (orig.getLinkType() != changed.getLinkType()) {
                            updateBuilder.updateColumnValue("linkType", changed.getLinkType());
                            orig.setLinkType(changed.getLinkType());
                            changedColumns++;
                        }

                        if (!orig.getFirstName().equals(changed.getFirstName())) {
                            updateBuilder.updateColumnValue("firstName", changed.getFirstName());
                            orig.setFirstName(changed.getFirstName());
                            changedColumns++;
                        }

                        if (!orig.getLastName().equals(changed.getLastName())) {
                            updateBuilder.updateColumnValue("lastName", changed.getLastName());
                            orig.setLastName(changed.getLastName());
                            changedColumns++;
                        }

                        if (!orig.getStatus().equals(changed.getStatus())) {
                            updateBuilder.updateColumnValue("status", changed.getStatus());
                            orig.setStatus(changed.getStatus());
                            changedColumns++;
                        }

                        if (!orig.getPhoto().equals(changed.getPhoto())) {
                            updateBuilder.updateColumnValue("photo", changed.getPhoto());
                            orig.setPhoto(changed.getPhoto());
                            changedColumns++;
                        }

                        userDao.update(orig);
                    }
                }
                Logger.d(TAG, "Updated columns: " + changedColumns);
                return null;
            }
        });

        Logger.d(TAG, "onUsers updated in " + (SystemClock.uptimeMillis() - start));

        if (application.getUserSource() != null) {
            for (Pair<User, User> user : diff) {
                application.getUserSource().notifyUserChanged(user.second.getUid());
            }
        }

        start = SystemClock.uptimeMillis();
        Logger.d(TAG, "onUsers notified in " + (SystemClock.uptimeMillis() - start) + " ms");
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
        User[] res = userDao.queryForEq("linkType", LinkType.CONTACT).toArray(new User[0]);
        for (int i = 0; i < res.length; i++) {
            res[i] = cacheUser(res[i]);
        }
        return res;
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
                            Contact contact = new Contact(uid, localId, getUser(uid), false);
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

                    }
                }
                if (updated.size() > 0) {
                    onUsers(updated.toArray(new User[updated.size()]));
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
