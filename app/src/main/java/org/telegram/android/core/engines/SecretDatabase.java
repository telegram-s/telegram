package org.telegram.android.core.engines;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.core.model.*;
import org.telegram.android.log.Logger;
import org.telegram.ormlite.OrmEncryptedChat;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 03.01.14.
 */
public class SecretDatabase {
    private static final String TAG = "SecretDatabase";

    private RuntimeExceptionDao<OrmEncryptedChat, Integer> secretDao;
    private ConcurrentHashMap<Integer, EncryptedChat> secretCache;

    public SecretDatabase(ModelEngine engine) {
        secretDao = engine.getDatabase().getEncryptedChatDao();
        secretCache = new ConcurrentHashMap<Integer, EncryptedChat>();
    }

    public EncryptedChat[] getPendingEncryptedChats() {
        QueryBuilder<OrmEncryptedChat, Integer> queryBuilder = secretDao.queryBuilder();
        try {
            queryBuilder.where().eq("state", EncryptedChatState.REQUESTED);
            OrmEncryptedChat[] encryptedChats = secretDao.query(queryBuilder.prepare()).toArray(new OrmEncryptedChat[0]);
            EncryptedChat[] res = new EncryptedChat[encryptedChats.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = cachedConvert(encryptedChats[i]);
            }
        } catch (SQLException e) {
            Logger.t(TAG, e);
        }
        return new EncryptedChat[0];
    }

    public EncryptedChat loadChat(int id) {
        EncryptedChat res = secretCache.get(id);
        if (res != null)
            return res;

        return cachedConvert(secretDao.queryForId(id));
    }

    public void deleteChat(int chatId) {
        secretDao.deleteById(chatId);
        secretCache.remove(chatId);
    }

    public void updateChat(EncryptedChat chat) {
        OrmEncryptedChat encryptedChat = secretDao.queryForId(chat.getId());
        applyData(chat, encryptedChat);
        secretDao.update(encryptedChat);
    }

    public void updateOrCreateChat(EncryptedChat chat) {
        OrmEncryptedChat encryptedChat = new OrmEncryptedChat();
        applyData(chat, encryptedChat);
        secretDao.createOrUpdate(encryptedChat);
    }

    public void createChat(EncryptedChat chat) {
        OrmEncryptedChat encryptedChat = new OrmEncryptedChat();
        applyData(chat, encryptedChat);
        secretDao.create(encryptedChat);
    }

    private void applyData(EncryptedChat src, OrmEncryptedChat dest) {
        dest.setId(src.getId());
        dest.setUserId(src.getUserId());
        dest.setKey(src.getKey());
        dest.setOut(src.isOut());
        dest.setSelfDestructTime(src.getSelfDestructTime());
        dest.setAccessHash(src.getAccessHash());
    }

    private EncryptedChat cachedConvert(OrmEncryptedChat src) {
        if (src == null) {
            return null;
        }

        synchronized (src) {
            EncryptedChat res = secretCache.get(src.getId());
            if (res == null) {
                res = new EncryptedChat();
                secretCache.putIfAbsent(src.getId(), res);
                res = secretCache.get(src.getId());
            }

            res.setAccessHash(src.getAccessHash());
            res.setId(src.getId());
            res.setKey(src.getKey());
            res.setOut(src.isOut());
            res.setSelfDestructTime(src.getSelfDestructTime());
            res.setState(src.getState());
            res.setUserId(src.getUserId());
        }

        return secretCache.get(src.getId());
    }
}