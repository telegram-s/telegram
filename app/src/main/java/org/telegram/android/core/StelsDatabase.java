package org.telegram.android.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.User;
import org.telegram.android.log.Logger;
import org.telegram.ormlite.*;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:17
 */
public class StelsDatabase extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "Database";

    private static final String DATABASE_NAME = "stels.db";
    private static final int DATABASE_VERSION = 57;

    private RuntimeExceptionDao<FullChatInfo, Long> fullChatInfoDao;
    private RuntimeExceptionDao<Contact, Long> contactsDao;
    private RuntimeExceptionDao<MediaRecord, Long> mediaDao;

    private boolean wasUpgraded = false;

    public StelsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        getWritableDatabase().execSQL("PRAGMA synchronous = OFF;PRAGMA journal_mode = DELETE;");
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, OrmDialog.class);
            TableUtils.createTable(connectionSource, OrmUser.class);
            TableUtils.createTable(connectionSource, Contact.class);
            TableUtils.createTable(connectionSource, FullChatInfo.class);
            TableUtils.createTable(connectionSource, MediaRecord.class);
            TableUtils.createTable(connectionSource, OrmEncryptedChat.class);

            //User
            database.execSQL("CREATE UNIQUE INDEX mytest_id_idx ON ChatMessage(mid);\n");
        } catch (SQLException e) {
            Logger.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        wasUpgraded = true;
        try {
            TableUtils.dropTable(connectionSource, OrmDialog.class, true);
            TableUtils.dropTable(connectionSource, User.class, true);
            TableUtils.dropTable(connectionSource, Contact.class, true);
            TableUtils.dropTable(connectionSource, FullChatInfo.class, true);
            TableUtils.dropTable(connectionSource, MediaRecord.class, true);
            TableUtils.dropTable(connectionSource, OrmEncryptedChat.class, true);

            TableUtils.createTable(connectionSource, OrmDialog.class);
            TableUtils.createTable(connectionSource, User.class);
            TableUtils.createTable(connectionSource, Contact.class);
            TableUtils.createTable(connectionSource, FullChatInfo.class);
            TableUtils.createTable(connectionSource, MediaRecord.class);
            TableUtils.createTable(connectionSource, OrmEncryptedChat.class);
            database.execSQL("CREATE UNIQUE INDEX mytest_id_idx ON ChatMessage(mid);\n");
        } catch (SQLException e) {
            Logger.e(TAG, "Can't upgrade databases", e);
            throw new RuntimeException(e);
        }
//
//        if (oldVersion < 47) {
//            try {
//                TableUtils.dropTable(connectionSource, DialogDescription.class, true);
//                TableUtils.dropTable(connectionSource, ChatMessage.class, true);
//                TableUtils.dropTable(connectionSource, org.telegram.ormlite.User.class, true);
//                TableUtils.dropTable(connectionSource, Contact.class, true);
//                TableUtils.dropTable(connectionSource, FullChatInfo.class, true);
//                TableUtils.dropTable(connectionSource, MediaRecord.class, true);
//                TableUtils.dropTable(connectionSource, EncryptedChat.class, true);
//
//                TableUtils.createTable(connectionSource, DialogDescription.class);
//                TableUtils.createTable(connectionSource, ChatMessage.class);
//                TableUtils.createTable(connectionSource, org.telegram.ormlite.User.class);
//                TableUtils.createTable(connectionSource, Contact.class);
//                TableUtils.createTable(connectionSource, FullChatInfo.class);
//                TableUtils.createTable(connectionSource, MediaRecord.class);
//                TableUtils.createTable(connectionSource, EncryptedChat.class);
//                database.execSQL("CREATE UNIQUE INDEX mytest_id_idx ON ChatMessage(mid);\n");
//            } catch (SQLException e) {
//                Logger.e(TAG, "Can't upgrade databases", e);
//                throw new RuntimeException(e);
//            }
//        } else {
//            if (oldVersion == 47) {
//                database.execSQL("ALTER TABLE encryptedchat ADD COLUMN isOut SMALLINT");
//            }
//            if (oldVersion < 53) {
//                database.execSQL("ALTER TABLE dialogdescription ADD COLUMN firstUnreadMessage INTEGER");
//            }
//            if (oldVersion < 54) {
//                database.execSQL("CREATE INDEX dlg_date_idx ON dialogdescription(date);\n");
//            }
//        }
    }

    public void clearData() {
        try {
            TransactionManager.callInTransaction(connectionSource,
                    new Callable<Void>() {
                        public Void call() throws Exception {
                            TableUtils.clearTable(connectionSource, OrmDialog.class);
                            TableUtils.clearTable(connectionSource, OrmUser.class);
                            TableUtils.clearTable(connectionSource, Contact.class);
                            TableUtils.clearTable(connectionSource, FullChatInfo.class);
                            TableUtils.clearTable(connectionSource, MediaRecord.class);
                            TableUtils.clearTable(connectionSource, OrmEncryptedChat.class);
                            return null;
                        }
                    });
        } catch (Exception e) {
            Logger.t(TAG, e);
        }
    }

    public boolean isWasUpgraded() {
        return wasUpgraded;
    }

    public RuntimeExceptionDao<MediaRecord, Long> getMediaDao() {
        if (mediaDao == null) {
            mediaDao = getRuntimeExceptionDao(MediaRecord.class);
        }
        return mediaDao;
    }

    public RuntimeExceptionDao<Contact, Long> getContactsDao() {
        if (contactsDao == null) {
            contactsDao = getRuntimeExceptionDao(Contact.class);
        }
        return contactsDao;
    }

    public RuntimeExceptionDao<FullChatInfo, Long> getFullChatInfoDao() {
        if (fullChatInfoDao == null) {
            fullChatInfoDao = getRuntimeExceptionDao(FullChatInfo.class);
        }

        return fullChatInfoDao;
    }
}
