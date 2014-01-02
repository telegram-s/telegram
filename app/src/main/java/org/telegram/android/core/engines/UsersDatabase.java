package org.telegram.android.core.engines;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class UsersDatabase {

    private static final String TAG = "UsersDatabase";

    private RuntimeExceptionDao<org.telegram.ormlite.User, Integer> userDao;

    private ConcurrentHashMap<Integer, org.telegram.ormlite.User> userDbCache;
    private ConcurrentHashMap<Integer, User> userCache;

    private StelsApplication application;

    public UsersDatabase(StelsApplication application, ModelEngine engine) {
        this.application = application;
        userDao = engine.getDatabase().getUsersDao();
        userDbCache = new ConcurrentHashMap<Integer, org.telegram.ormlite.User>();
        userCache = new ConcurrentHashMap<Integer, User>();
    }

    public User[] getContacts() {
        org.telegram.ormlite.User[] dbResult = userDao.queryForEq("linkType", LinkType.CONTACT).toArray(new org.telegram.ormlite.User[0]);
        User[] res = new User[dbResult.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = cachedConvert(dbResult[i]);
        }
        return res;
    }

    public User getUser(int uid) {
        User res = userCache.get(uid);
        if (res != null)
            return res;

        return cachedConvert(userDao.queryForId(uid));
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
                QueryBuilder<org.telegram.ormlite.User, Integer> queryBuilder = userDao.queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                org.telegram.ormlite.User[] res = queryBuilder.query().toArray(new org.telegram.ormlite.User[0]);
                Logger.d(TAG, "Loaded users in " + (System.currentTimeMillis() - start) + " ms");
                for (int i = 0; i < res.length; i++) {
                    users.add(cachedConvert(res[i]));
                }
            }
            return users.toArray(new User[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new User[0];
    }

    private org.telegram.ormlite.User[] getDbUsersById(Object[] uid) {
        try {
            ArrayList<Object> missedMids = new ArrayList<Object>();
            ArrayList<org.telegram.ormlite.User> users = new ArrayList<org.telegram.ormlite.User>();
            for (Object o : uid) {
                org.telegram.ormlite.User u = userDbCache.get(o);
                if (u != null) {
                    users.add(u);
                } else {
                    missedMids.add(o);
                }
            }
            if (missedMids.size() > 0) {
                long start = System.currentTimeMillis();
                QueryBuilder<org.telegram.ormlite.User, Integer> queryBuilder = userDao.queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                org.telegram.ormlite.User[] res = queryBuilder.query().toArray(new org.telegram.ormlite.User[0]);
                Logger.d(TAG, "Loaded db users in " + (System.currentTimeMillis() - start) + " ms");
                for (int i = 0; i < res.length; i++) {
                    users.add(cache(res[i]));
                }
            }
            return users.toArray(new org.telegram.ormlite.User[0]);
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }

        return new org.telegram.ormlite.User[0];
    }

    public void updateUsers(User... users) {
        if (users.length == 0) {
            return;
        }

        org.telegram.ormlite.User[] converted = new org.telegram.ormlite.User[users.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = new org.telegram.ormlite.User();
            converted[i].setUid(users[i].getUid());
            converted[i].setAccessHash(users[i].getAccessHash());
            converted[i].setFirstName(users[i].getFirstName());
            converted[i].setLastName(users[i].getLastName());
            converted[i].setPhone(users[i].getPhone());
            converted[i].setStatus(users[i].getStatus());
            converted[i].setLinkType(users[i].getLinkType());
        }

        updateUsers(converted);
    }

    private void updateUsers(org.telegram.ormlite.User[] users) {
        Logger.d(TAG, "updateUsers: " + users.length);

        final ArrayList<Pair<org.telegram.ormlite.User, org.telegram.ormlite.User>> diff = new ArrayList<Pair<org.telegram.ormlite.User, org.telegram.ormlite.User>>();
        int changed = 0;

        long start = SystemClock.uptimeMillis();
        Integer[] ids = new Integer[users.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = users[i].getUid();
        }
        org.telegram.ormlite.User[] original = getDbUsersById(ids);

        Logger.d(TAG, "onUsers original in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();

        for (org.telegram.ormlite.User m : users) {
            org.telegram.ormlite.User orig = searchUser(original, m.getUid());

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
                diff.add(new Pair<org.telegram.ormlite.User, org.telegram.ormlite.User>(orig, m));
            }

        }

        Logger.d(TAG, "onUsers changed " + changed + " of " + users.length);

        if (changed == 0) {
            return;
        }

        Logger.d(TAG, "onUsers prepared in " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        userDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                int changedColumns = 0;
                for (Pair<org.telegram.ormlite.User, org.telegram.ormlite.User> user : diff) {
                    org.telegram.ormlite.User orig = user.first;
                    org.telegram.ormlite.User changed = user.second;

                    if (orig == null) {
                        userDao.create(changed);
                        userDbCache.putIfAbsent(changed.getUid(), changed);
                    } else {
                        UpdateBuilder<org.telegram.ormlite.User, Integer> updateBuilder = userDao.updateBuilder();
                        updateBuilder.where().eq("uid", orig.getUid());

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

                        if ((orig.getPhoto() == null && changed.getPhoto() == null) ||
                                (orig.getPhoto() != null && !orig.getPhoto().equals(changed.getPhoto()))) {
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
            for (Pair<org.telegram.ormlite.User, org.telegram.ormlite.User> user : diff) {
                application.getUserSource().notifyUserChanged(user.second.getUid());
            }
        }

        start = SystemClock.uptimeMillis();
        Logger.d(TAG, "onUsers notified in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    private User cachedConvert(org.telegram.ormlite.User user) {
        user = cache(user);

        synchronized (user) {
            User res = userCache.get(user.getUid());
            if (res == null) {
                res = new User();
            }
            res.setUid(user.getUid());
            res.setAccessHash(user.getAccessHash());
            res.setFirstName(user.getFirstName());
            res.setLastName(user.getLastName());
            res.setPhoto(user.getPhoto());
            res.setPhone(user.getPhone());
            res.setStatus(user.getStatus());
            res.setLinkType(user.getLinkType());
            userCache.putIfAbsent(res.getUid(), res);
        }

        return userCache.get(user.getUid());
    }

    private org.telegram.ormlite.User cache(org.telegram.ormlite.User user) {
        userDbCache.putIfAbsent(user.getUid(), user);
        return userDbCache.get(user.getUid());
    }

    public static User searchUser(User[] users, long id) {
        for (User user : users) {
            if (user.getUid() == id)
                return user;
        }

        return null;
    }

    public static org.telegram.ormlite.User searchUser(org.telegram.ormlite.User[] users, long id) {
        for (org.telegram.ormlite.User user : users) {
            if (user.getUid() == id)
                return user;
        }

        return null;
    }

    public void clearCache() {
        userDbCache.clear();
        userCache.clear();
    }
}
