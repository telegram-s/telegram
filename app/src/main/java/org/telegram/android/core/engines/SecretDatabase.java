package org.telegram.android.core.engines;

import org.telegram.android.core.model.*;
import org.telegram.dao.SecretChat;
import org.telegram.dao.SecretChatDao;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 03.01.14.
 */
public class SecretDatabase {
    private static final String TAG = "SecretDatabase";

    private SecretChatDao secretDao;
    private ConcurrentHashMap<Integer, EncryptedChat> secretCache;

    public SecretDatabase(ModelEngine engine) {
        secretDao = engine.getDaoSession().getSecretChatDao();
        secretCache = new ConcurrentHashMap<Integer, EncryptedChat>();
    }

    public EncryptedChat[] getPendingEncryptedChats() {
        SecretChat[] encryptedChats = secretDao.queryBuilder()
                .where(SecretChatDao.Properties.State.eq(EncryptedChatState.REQUESTED))
                .list().toArray(new SecretChat[0]);
        EncryptedChat[] res = new EncryptedChat[encryptedChats.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = cachedConvert(encryptedChats[i]);
        }
        return res;
    }

    public EncryptedChat[] loadChats(int[] ids) {
        if (ids.length == 0) {
            return new EncryptedChat[0];
        }

        Integer[] bids = new Integer[ids.length];
        for (int i = 0; i < bids.length; i++) {
            bids[i] = ids[i];
        }

        return loadChats(bids);
    }

    public EncryptedChat[] loadChats(Integer[] ids) {
        if (ids.length == 0) {
            return new EncryptedChat[0];
        }
        SecretChat[] chats = secretDao.queryBuilder()
                .where(SecretChatDao.Properties.Id.in((Object[]) ids))
                .list()
                .toArray(new SecretChat[0]);
        EncryptedChat[] res = new EncryptedChat[chats.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = cachedConvert(chats[i]);
        }
        return res;
    }

    public EncryptedChat loadChat(int id) {
        EncryptedChat res = secretCache.get(id);
        if (res != null)
            return res;

        return cachedConvert(secretDao.load((long) id));
    }

    public void deleteChat(int chatId) {
        secretDao.deleteByKey((long) chatId);
        secretCache.remove(chatId);
    }

    public void updateChat(EncryptedChat chat) {
        SecretChat encryptedChat = secretDao.load((long) chat.getId());
        applyData(chat, encryptedChat);
        secretDao.update(encryptedChat);
    }

    public void updateOrCreateChat(EncryptedChat chat) {
        SecretChat encryptedChat = new SecretChat();
        applyData(chat, encryptedChat);
        secretDao.insertOrReplace(encryptedChat);
    }

    public void createChat(EncryptedChat chat) {
        SecretChat encryptedChat = new SecretChat();
        applyData(chat, encryptedChat);
        secretDao.insert(encryptedChat);
    }

    private void applyData(EncryptedChat src, SecretChat dest) {
        dest.setId(src.getId());
        dest.setUid(src.getUserId());
        dest.setKey(src.getKey());
        dest.setIsOut(src.isOut());
        dest.setSelfDestruct(src.getSelfDestructTime());
        dest.setAccessHash(src.getAccessHash());
        dest.setState(src.getState());
    }

    private EncryptedChat cachedConvert(SecretChat src) {
        if (src == null) {
            return null;
        }

        synchronized (src) {
            EncryptedChat res = secretCache.get((int) (long) src.getId());
            if (res == null) {
                res = new EncryptedChat();
                secretCache.putIfAbsent((int) (long) src.getId(), res);
                res = secretCache.get((int) (long) src.getId());
            }

            res.setAccessHash(src.getAccessHash());
            res.setId((int) src.getId());
            res.setKey(src.getKey());
            res.setOut(src.getIsOut());
            res.setSelfDestructTime(src.getSelfDestruct());
            res.setState(src.getState());
            res.setUserId(src.getUid());
        }

        return secretCache.get((int) (long) src.getId());
    }

    public void clear() {
        secretDao.deleteAll();
        secretCache.clear();
    }
}