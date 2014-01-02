package org.telegram.android.core.engines;

import android.os.SystemClock;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;
import org.telegram.ormlite.OrmDialog;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class DialogsDatabase {
    private static final String TAG = "DialogsDatabase";

    private RuntimeExceptionDao<OrmDialog, Long> dialogsDao;

    private ConcurrentHashMap<Long, OrmDialog> dialogsDbCache;
    private ConcurrentHashMap<Long, DialogDescription> dialogsCache;

    public DialogsDatabase(ModelEngine engine) {
        dialogsDao = engine.getDatabase().getDialogsDao();
        dialogsDbCache = new ConcurrentHashMap<Long, OrmDialog>();
        dialogsCache = new ConcurrentHashMap<Long, DialogDescription>();
    }

    public DialogDescription[] getItems(int offset, int limit) {
        try {
            long start = SystemClock.uptimeMillis();
            QueryBuilder<OrmDialog, Long> queryBuilder = dialogsDao.queryBuilder();
            Logger.d(TAG, "Builder created in " + (SystemClock.uptimeMillis() - start) + " ms");
            queryBuilder.orderBy("date", false);
            queryBuilder.where().ne("date", 0);
            queryBuilder.offset(offset);
            queryBuilder.limit(limit);
            List<OrmDialog> dialogDescriptions = dialogsDao.query(queryBuilder.prepare());
            Logger.d(TAG, "Queried in " + (SystemClock.uptimeMillis() - start) + " ms");
            OrmDialog[] res = dialogDescriptions.toArray(new OrmDialog[dialogDescriptions.size()]);
            Logger.d(TAG, "Loaded items in " + (SystemClock.uptimeMillis() - start) + " ms");
            DialogDescription[] dRes = new DialogDescription[res.length];
            for (int i = 0; i < dRes.length; i++) {
                dRes[i] = convertCached(res[i]);
            }
            return dRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DialogDescription[] getAll() {
        try {
            long start = SystemClock.uptimeMillis();
            QueryBuilder<OrmDialog, Long> queryBuilder = dialogsDao.queryBuilder();
            Logger.d(TAG, "Builder created in " + (SystemClock.uptimeMillis() - start) + " ms");
            queryBuilder.where().ne("date", 0);
            List<OrmDialog> dialogDescriptions = dialogsDao.query(queryBuilder.prepare());
            Logger.d(TAG, "Queried in " + (SystemClock.uptimeMillis() - start) + " ms");
            OrmDialog[] res = dialogDescriptions.toArray(new OrmDialog[dialogDescriptions.size()]);
            Logger.d(TAG, "Loaded items in " + (SystemClock.uptimeMillis() - start) + " ms");
            DialogDescription[] dRes = new DialogDescription[res.length];
            for (int i = 0; i < dRes.length; i++) {
                dRes[i] = convertCached(res[i]);
            }
            return dRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DialogDescription[] getUnreadedRemotelyDescriptions() {
        return new DialogDescription[0];
    }

    public OrmDialog loadDialogDb(int peerType, int peerId) {
        try {
            QueryBuilder<OrmDialog, Long> queryBuilder = dialogsDao.queryBuilder();
            queryBuilder.where().eq("peerType", peerType).and().eq("peerId", peerId);
            return dialogsDao.queryForFirst(queryBuilder.prepare());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DialogDescription loadDialog(int peerType, int peerId) {
        long id = uniqId(peerType, peerId);
        DialogDescription description = dialogsCache.get(id);
        if (description != null) {
            return description;
        }
        return convertCached(loadDialogDb(peerType, peerId));
    }

    public void createDialog(DialogDescription description) {
        OrmDialog dialog = new OrmDialog();
        applyChanges(description, dialog);
        dialogsDao.create(dialog);
    }

    public void updateDialog(DialogDescription description) {
        OrmDialog dialog = loadDialogDb(description.getPeerType(), description.getPeerId());
        applyChanges(description, dialog);
        dialogsDao.update(dialog);
    }

    public void deleteDialog(DialogDescription description) {
        OrmDialog dialog = loadDialogDb(description.getPeerType(), description.getPeerId());
        dialog.setDate(0);
        dialogsDao.update(dialog);
    }

    private DialogDescription convertCached(OrmDialog dialog) {
        if (dialog == null) {
            return null;
        }

        long id = uniqId(dialog);
        synchronized (dialog) {
            DialogDescription res = dialogsCache.get(id);
            if (res == null) {
                res = new DialogDescription();
                dialogsCache.putIfAbsent(id, res);
                res = dialogsCache.get(id);
            }

            res.setPeerType(dialog.getPeerType());
            res.setPeerId(dialog.getPeerId());
            res.setContentType(dialog.getContentType());
            res.setDate(dialog.getDate());
            res.setExtras(dialog.getExtras());
            res.setFailure(dialog.isFailure());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            res.setLastLocalViewedMessage(dialog.getLastLocalViewedMessage());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            res.setMessage(dialog.getMessage());
            res.setMessageState(dialog.getMessageState());
            res.setSenderId(dialog.getSenderId());
            res.setSenderTitle(dialog.getSenderTitle());
            res.setPhoto(dialog.getPhoto());
            res.setTopMessageId(dialog.getTopMessageId());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            res.setTitle(dialog.getTitle());
            res.setSenderTitle(dialog.getSenderTitle());
        }

        return dialogsCache.get(id);
    }

    private void applyChanges(DialogDescription src, OrmDialog dest) {
        dest.setPeerType(src.getPeerType());
        dest.setPeerId(src.getPeerId());
        dest.setContentType(src.getContentType());
        dest.setDate(src.getDate());
        dest.setExtras(src.getExtras());
        dest.setFailure(src.isFailure());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setLastLocalViewedMessage(src.getLastLocalViewedMessage());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setMessage(src.getMessage());
        dest.setMessageState(src.getMessageState());
        dest.setSenderId(src.getSenderId());
        dest.setSenderTitle(src.getSenderTitle());
        dest.setPhoto(src.getPhoto());
        dest.setTopMessageId(src.getTopMessageId());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setTitle(src.getTitle());
    }

    private long uniqId(DialogDescription description) {
        return description.getPeerType() + description.getPeerId() * 10L;
    }

    private long uniqId(OrmDialog description) {
        return description.getPeerType() + description.getPeerId() * 10L;
    }

    private long uniqId(int peerType, int peerId) {
        return peerType + peerId * 10L;
    }
}
