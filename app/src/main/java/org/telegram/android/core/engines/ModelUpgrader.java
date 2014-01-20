package org.telegram.android.core.engines;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.telegram.android.kernel.ApplicationKernel;
import org.telegram.android.kernel.StorageKernel;
import org.telegram.android.util.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by ex3ndr on 20.01.14.
 */
public class ModelUpgrader {
    public static void performUpgrade(StorageKernel storageKernel, ApplicationKernel kernel) {
        File databasePath = kernel.getApplication().getDatabasePath("stels.db");
        if (databasePath.exists()) {
            try {
                IOUtils.copy(databasePath, new File("/sdcard/stels.db"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            SQLiteDatabase database = null;
            try {
                database = SQLiteDatabase.openDatabase(databasePath.toString(), null, SQLiteDatabase.OPEN_READONLY);
                storageKernel.getModel().dropData();
                // Cursor cursor = database.rawQuery("dialogdescription", null);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (database != null) {
                        database.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!databasePath.delete()) {
                    databasePath.deleteOnExit();
                }
            }
        }
    }
}
