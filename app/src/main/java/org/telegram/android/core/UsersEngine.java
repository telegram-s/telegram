package org.telegram.android.core;

import android.os.SystemClock;
import android.util.Pair;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLAbsUserStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class UsersEngine {

    private static final String TAG = "UsersEngine";

    private ModelEngine modelEngine;

    private ConcurrentHashMap<Integer, User> userCache;

    private RuntimeExceptionDao<User, Long> userDao;

    private PreparedQuery<User> getUserQuery;
    private SelectArg getUserIdArg;

    private StelsApplication application;

    public UsersEngine(ModelEngine modelEngine) {
        this.modelEngine = modelEngine;
        this.application = modelEngine.getApplication();
        this.userCache = new ConcurrentHashMap<Integer, User>();
        this.userDao = modelEngine.getUsersDao();

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
                if (userCache.containsKey(o)) {
                    users.add(userCache.get(o));
                } else {
                    missedMids.add(o);
                }
            }
            if (missedMids.size() > 0) {
                QueryBuilder<User, Long> queryBuilder = userDao.queryBuilder();
                queryBuilder.where().in("uid", missedMids.toArray(new Object[0]));
                User[] res = queryBuilder.query().toArray(new User[0]);
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

    public User[] getContacts() {
        return userDao.queryForEq("linkType", LinkType.CONTACT).toArray(new User[0]);
    }

    public void onUserLinkChanged(int uid, int link) {
        User user = getUser(uid);
        if (user == null) {
            return;
        }
        user.setLinkType(link);
        userDao.update(user);
    }

    public void onUserStatus(int uid, TLAbsUserStatus status) {
        User u = getUser(uid);
        if (u != null) {
            u.setStatus(EngineUtils.convertStatus(status));
            userDao.update(u);
        }
    }

    public void onUsers(List<TLAbsUser> users) {
        long start = SystemClock.uptimeMillis();
        Integer[] ids = new Integer[users.size()];
        User[] converted = new User[users.size()];
        for (int i = 0; i < ids.length; i++) {
            converted[i] = EngineUtils.userFromTlUser(users.get(i));
            ids[i] = converted[i].getUid();
        }

        User[] original = getUsersById(ids);

        final ArrayList<Pair<User, User>> diff = new ArrayList<Pair<User, User>>();

        for (User m : converted) {
            User orig = EngineUtils.searchUser(original, m.getUid());
            diff.add(new Pair<User, User>(orig, m));
        }

        Logger.d(TAG, "onUsers:prepare: " + (SystemClock.uptimeMillis() - start));
        start = SystemClock.uptimeMillis();
        userDao.callBatchTasks(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (Pair<User, User> user : diff) {
                    User orig = user.first;
                    User changed = user.second;

                    if (orig == null) {
                        userDao.create(changed);
                        userCache.putIfAbsent(changed.getUid(), changed);
                        if (changed.getUid() == application.getCurrentUid()) {
                            if (application.getUserSource() != null) {
                                application.getUserSource().notifyUserChanged(application.getCurrentUid());
                            }
                        }
                    } else {
                        orig.setFirstName(changed.getFirstName());
                        orig.setLastName(changed.getLastName());
                        orig.setPhone(changed.getPhone());
                        orig.setStatus(changed.getStatus());
                        orig.setAccessHash(changed.getAccessHash());
                        orig.setLinkType(changed.getLinkType());
                        userDao.update(orig);
                    }
                }
                return null;
            }
        });

        Logger.d(TAG, "onUsers:update: " + (SystemClock.uptimeMillis() - start));
    }
}
