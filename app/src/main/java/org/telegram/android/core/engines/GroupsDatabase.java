package org.telegram.android.core.engines;

import org.telegram.android.core.model.Group;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.dao.GroupChat;
import org.telegram.dao.GroupChatDao;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 04.01.14.
 */
public class GroupsDatabase {
    private ConcurrentHashMap<Integer, Group> groupCache;
    private GroupChatDao groupDao;

    public GroupsDatabase(ModelEngine engine) {
        groupDao = engine.getDaoSession().getGroupChatDao();
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
        org.telegram.dao.GroupChat[] converted = new org.telegram.dao.GroupChat[groups.length];
        for (int i = 0; i < converted.length; i++) {
            converted[i] = new org.telegram.dao.GroupChat();
            converted[i].setId(groups[i].getChatId());
            converted[i].setIsForbidden(groups[i].isForbidden());
            converted[i].setUsersCount(groups[i].getUsersCount());
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

    private void updateGroups(org.telegram.dao.GroupChat... groups) {
        groupDao.insertOrReplaceInTx(groups);
    }

    private Group cachedConvert(GroupChat group) {
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
            res.setForbidden(group.getIsForbidden());
            res.setUsersCount(group.getUsersCount());
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
