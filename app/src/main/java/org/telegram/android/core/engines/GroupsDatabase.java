package org.telegram.android.core.engines;

import org.telegram.android.core.model.Group;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.dao.GroupDao;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class GroupsDatabase {
    private ConcurrentHashMap<Integer, Group> groupCache;
    private GroupDao groupDao;

    public GroupsDatabase(ModelEngine engine) {
        groupDao = engine.getDaoSession().getGroupDao();
        groupCache = new ConcurrentHashMap<Integer, Group>();
    }

    public Group getGroup(int groupId) {
        Group res = groupCache.get(groupId);
        if (res != null)
            return res;

        return cachedConvert(groupDao.load((long) groupId));
    }

    public void updateGroups(Group... groups) {
        if (groups.length == 0) {
            return;
        }
        org.telegram.dao.Group[] converted = new org.telegram.dao.Group[groups.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = new org.telegram.dao.Group();
            converted[i].setId(groups[i].getChatId());
            if (groups[i].getAvatar() != null) {
                try {
                    converted[i].setAvatar(groups[i].getAvatar().serialize());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            converted[i].setTitle(groups[i].getTitle());
        }

        updateGroups(converted);
    }

    private void updateGroups(org.telegram.dao.Group... groups) {
        groupDao.insertOrReplaceInTx(groups);
    }

    private Group cachedConvert(org.telegram.dao.Group group) {
        if (group == null) {
            return null;
        }

        // user = cache(user);
        synchronized (group) {
            Group res = groupCache.get((int) (long) group.getId());
            if (res == null) {
                res = new Group();
                groupCache.putIfAbsent((int) (long) group.getId(), res);
                res = groupCache.get((int) (long) group.getId());
            }
            res.setChatId((int) group.getId());
            res.setTitle(group.getTitle());
            if (group.getAvatar() != null) {
                try {
                    res.setAvatar((TLAbsLocalAvatarPhoto) TLLocalContext.getInstance().deserializeMessage(group.getAvatar()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return groupCache.get((int) (long) group.getId());
    }
}
