package org.telegram.android.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.telegram.android.R;
import org.telegram.android.core.model.*;
import org.telegram.android.log.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 18:17
 */
public class StelsDatabase extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "Database";

    private static final String DATABASE_NAME = "stels.db";
    private static final int DATABASE_VERSION = 52;

    private RuntimeExceptionDao<DialogDescription, Long> dialogsDao;
    private RuntimeExceptionDao<FullChatInfo, Long> fullChatInfoDao;
    private RuntimeExceptionDao<EncryptedChat, Long> encryptedChats;
    private RuntimeExceptionDao<ChatMessage, Long> messagesDao;
    private RuntimeExceptionDao<User, Long> usersDao;
    private RuntimeExceptionDao<Contact, Long> contactsDao;
    private RuntimeExceptionDao<MediaRecord, Long> mediaDao;

    private boolean wasUpgraded = false;

    public StelsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);

        getWritableDatabase().execSQL("PRAGMA synchronous = OFF;");
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, DialogDescription.class);
            TableUtils.createTable(connectionSource, ChatMessage.class);
            TableUtils.createTable(connectionSource, User.class);
            TableUtils.createTable(connectionSource, Contact.class);
            TableUtils.createTable(connectionSource, FullChatInfo.class);
            TableUtils.createTable(connectionSource, MediaRecord.class);
            TableUtils.createTable(connectionSource, EncryptedChat.class);

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
            TableUtils.dropTable(connectionSource, DialogDescription.class, true);
            TableUtils.dropTable(connectionSource, ChatMessage.class, true);
            TableUtils.dropTable(connectionSource, User.class, true);
            TableUtils.dropTable(connectionSource, Contact.class, true);
            TableUtils.dropTable(connectionSource, FullChatInfo.class, true);
            TableUtils.dropTable(connectionSource, MediaRecord.class, true);
            TableUtils.dropTable(connectionSource, EncryptedChat.class, true);

            TableUtils.createTable(connectionSource, DialogDescription.class);
            TableUtils.createTable(connectionSource, ChatMessage.class);
            TableUtils.createTable(connectionSource, User.class);
            TableUtils.createTable(connectionSource, Contact.class);
            TableUtils.createTable(connectionSource, FullChatInfo.class);
            TableUtils.createTable(connectionSource, MediaRecord.class);
            TableUtils.createTable(connectionSource, EncryptedChat.class);
            database.execSQL("CREATE UNIQUE INDEX mytest_id_idx ON ChatMessage(mid);\n");
        } catch (SQLException e) {
            Logger.e(TAG, "Can't upgrade databases", e);
            throw new RuntimeException(e);
        }

//        if (oldVersion < 47) {
//            try {
//                TableUtils.dropTable(connectionSource, DialogDescription.class, true);
//                TableUtils.dropTable(connectionSource, ChatMessage.class, true);
//                TableUtils.dropTable(connectionSource, User.class, true);
//                TableUtils.dropTable(connectionSource, Contact.class, true);
//                TableUtils.dropTable(connectionSource, FullChatInfo.class, true);
//                TableUtils.dropTable(connectionSource, MediaRecord.class, true);
//                TableUtils.dropTable(connectionSource, EncryptedChat.class, true);
//
//                TableUtils.createTable(connectionSource, DialogDescription.class);
//                TableUtils.createTable(connectionSource, ChatMessage.class);
//                TableUtils.createTable(connectionSource, User.class);
//                TableUtils.createTable(connectionSource, Contact.class);
//                TableUtils.createTable(connectionSource, FullChatInfo.class);
//                TableUtils.createTable(connectionSource, MediaRecord.class);
//                TableUtils.createTable(connectionSource, EncryptedChat.class);
//                database.execSQL("CREATE UNIQUE INDEX mytest_id_idx ON ChatMessage(mid);\n");
//            } catch (SQLException e) {
//                Logger.e(TAG, "Can't upgrade databases", e);
//                throw new RuntimeException(e);
//            }
//        } else if (oldVersion == 47) {
//            try {
//                List<String> strings = TableUtils.getCreateTableStatements(connectionSource, EncryptedChat.class);
//                strings.toArray();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            database.execSQL("ALTER TABLE encryptedchat ADD COLUMN isOut SMALLINT");
//        }
    }

    public void clearData() {
        try {
            TransactionManager.callInTransaction(connectionSource,
                    new Callable<Void>() {
                        public Void call() throws Exception {
                            TableUtils.clearTable(connectionSource, DialogDescription.class);
                            TableUtils.clearTable(connectionSource, ChatMessage.class);
                            TableUtils.clearTable(connectionSource, User.class);
                            TableUtils.clearTable(connectionSource, Contact.class);
                            TableUtils.clearTable(connectionSource, FullChatInfo.class);
                            TableUtils.clearTable(connectionSource, MediaRecord.class);
                            TableUtils.clearTable(connectionSource, EncryptedChat.class);
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

    public RuntimeExceptionDao<User, Long> getUsersDao() {
        if (usersDao == null) {
            usersDao = getRuntimeExceptionDao(User.class);
        }
        return usersDao;
    }

    public RuntimeExceptionDao<DialogDescription, Long> getDialogsDao() {
        if (dialogsDao == null) {
            dialogsDao = getRuntimeExceptionDao(DialogDescription.class);
        }
        return dialogsDao;
    }

    public RuntimeExceptionDao<ChatMessage, Long> getMessagesDao() {
        if (messagesDao == null) {
            messagesDao = getRuntimeExceptionDao(ChatMessage.class);
        }
        return messagesDao;
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

    public RuntimeExceptionDao<EncryptedChat, Long> getEncryptedChatDao() {
        if (encryptedChats == null) {
            encryptedChats = getRuntimeExceptionDao(EncryptedChat.class);
        }
        return encryptedChats;
    }
}
