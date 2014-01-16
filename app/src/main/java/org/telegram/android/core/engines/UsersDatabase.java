package org.telegram.android.core.engines;

import android.os.SystemClock;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.log.Logger;
import org.telegram.dao.DaoSession;
import org.telegram.dao.UserDao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class UsersDatabase {

    private static final String TAG = "UsersDatabase";

    private UserDao userGreenDao;

    private ConcurrentHashMap<Integer, User> userCache;

    private StelsApplication application;

    public UsersDatabase(StelsApplication application, ModelEngine engine) {
        this.application = application;
        DaoSession session = engine.getDaoSession();
        userGreenDao = session.getUserDao();
        userCache = new ConcurrentHashMap<Integer, User>();
    }

    public User[] getContacts() {
        List<org.telegram.dao.User> dbRes = userGreenDao.queryBuilder().where(UserDao.Properties.LinkType.eq(LinkType.CONTACT)).list();
        User[] res = new User[dbRes.size()];
        int index = 0;
        for (org.telegram.dao.User u : dbRes) {
            res[index++] = cachedConvert(u);
        }

        return res;
    }

    public User getUser(int uid) {
        User res = userCache.get(uid);
        if (res != null)
            return res;

        return cachedConvert(userGreenDao.load((long) uid));
    }

    public User[] getUsersById(Object[] uid) {
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
            for (org.telegram.dao.User u : userGreenDao.queryBuilder().where(UserDao.Properties.Id.in(missedMids)).list()) {
                users.add(cachedConvert(u));
            }
            Logger.d(TAG, "Users loaded in " + (System.currentTimeMillis() - start) + " ms");
        }
        return users.toArray(new User[0]);
    }

    public void updateUsers(User... users) {
        if (users.length == 0) {
            return;
        }

        org.telegram.dao.User[] converted = new org.telegram.dao.User[users.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = new org.telegram.dao.User();
            converted[i].setId((long) users[i].getUid());
            converted[i].setAccessHash(users[i].getAccessHash());
            converted[i].setFirstName(users[i].getFirstName());
            converted[i].setLastName(users[i].getLastName());
            converted[i].setPhone(users[i].getPhone());
            if (users[i].getPhoto() == null) {
                converted[i].setAvatar(null);
            } else {
                try {
                    converted[i].setAvatar(users[i].getPhoto().serialize());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (users[i].getStatus() == null) {
                converted[i].setStatus(null);
            } else {
                try {
                    converted[i].setStatus(users[i].getStatus().serialize());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            converted[i].setLinkType(users[i].getLinkType());
        }

        updateUsers(converted);
    }

    private void updateUsers(org.telegram.dao.User[] users) {
        Logger.d(TAG, "updateUsers: " + users.length);
        long start = SystemClock.uptimeMillis();
        userGreenDao.insertOrReplaceInTx(users);
        Logger.d(TAG, "onUsers updated in " + (SystemClock.uptimeMillis() - start) + " ms");
        User[] u = new User[users.length];
        for (int i = 0; i < u.length; i++) {
            u[i] = cachedConvert(users[i]);
        }
        if (application.getUserSource() != null) {
            application.getUserSource().notifyUsersChanged(u);
        }
    }

    private User cachedConvert(org.telegram.dao.User user) {
        if (user == null) {
            return null;
        }

        // user = cache(user);
        synchronized (user) {
            User res = userCache.get((int) (long) user.getId());
            if (res == null) {
                res = new User();
                userCache.putIfAbsent((int) (long) user.getId(), res);
                res = userCache.get((int) (long) user.getId());
            }
            res.setUid((int) (long) user.getId());
            res.setAccessHash(user.getAccessHash());
            res.setFirstName(user.getFirstName());
            res.setLastName(user.getLastName());
            if (user.getAvatar() != null) {
                try {
                    res.setPhoto((TLAbsLocalAvatarPhoto) TLLocalContext.getInstance().deserializeMessage(user.getAvatar()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            res.setPhone(user.getPhone());
            if (user.getStatus() != null) {
                try {
                    res.setStatus((TLAbsLocalUserStatus) TLLocalContext.getInstance().deserializeMessage(user.getStatus()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            res.setLinkType(user.getLinkType());

        }

        return userCache.get((int) (long) user.getId());
    }

    public static User searchUser(User[] users, long id) {
        for (User user : users) {
            if (user.getUid() == id)
                return user;
        }

        return null;
    }

    public static org.telegram.dao.User searchUser(org.telegram.dao.User[] users, long id) {
        for (org.telegram.dao.User user : users) {
            if (user.getId() == id)
                return user;
        }

        return null;
    }

    public void clear() {
        userGreenDao.deleteAll();
        userCache.clear();
    }
}
