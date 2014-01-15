package org.telegram.android.core.engines;

import org.telegram.android.core.model.FullChatInfo;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.local.TLLocalFullChatInfo;
import org.telegram.dao.FullGroup;
import org.telegram.dao.FullGroupDao;

import java.io.IOException;

/**
 * Created by ex3ndr on 15.01.14.
 */
public class FullGroupDatabase {

    private FullGroupDao fullGroupDao;

    public FullGroupDatabase(ModelEngine modelEngine) {
        this.fullGroupDao = modelEngine.getDaoSession().getFullGroupDao();
    }

    public void delete(int chatId) {
        fullGroupDao.deleteByKey((long) chatId);
    }

    public FullChatInfo loadFullChatInfo(int chatId) {
        return convert(fullGroupDao.load((long) chatId));
    }

    public synchronized void updateOrCreate(FullChatInfo... infos) {
        if (infos.length == 0) {
            return;
        }

        FullGroup[] cov = new FullGroup[infos.length];
        for (int i = 0; i < cov.length; i++) {
            cov[i] = convert(infos[i]);
        }
        if (cov.length == 1) {
            fullGroupDao.insertOrReplace(cov[0]);
        } else {
            fullGroupDao.insertOrReplaceInTx(cov);
        }
    }

    public FullChatInfo convert(FullGroup fullGroup) {
        if (fullGroup == null) {
            return null;
        }
        try {
            return new FullChatInfo((int) fullGroup.getId(),
                    (TLLocalFullChatInfo) TLLocalContext.getInstance().deserializeMessage(fullGroup.getUsers()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public FullGroup convert(FullChatInfo fullGroup) {
        if (fullGroup == null) {
            return null;
        }
        try {
            return new FullGroup(fullGroup.getChatId(), fullGroup.getChatInfo().serialize());
        } catch (IOException e) {
            return null;
        }
    }
}
