package org.telegram.android.kernel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.telegram.android.R;
import org.telegram.android.core.engines.ModelEngine;
import org.telegram.android.core.engines.ModelUpgrader;
import org.telegram.android.log.Logger;

import java.io.File;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class StorageKernel {

    public static boolean requiredDatabaseUpgrade(ApplicationKernel kernel) {
        if (kernel.getApplication().getDatabasePath("stels.db").exists()) {
            return true;
        }
//        File databasePath = kernel.getApplication().getDatabasePath("telegram.db");
//        if (databasePath.exists()) {
//            SQLiteDatabase database = SQLiteDatabase.openDatabase("telegram.db", null, SQLiteDatabase.OPEN_READONLY);
//        }
        return false;
    }

    private static final String TAG = "StorageKernel";

    private ApplicationKernel kernel;
    private ModelEngine model;

    public StorageKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        long start = System.currentTimeMillis();
        this.model = new ModelEngine(kernel.getApplication());
        Logger.d(TAG, "ModelEngine loaded in " + (System.currentTimeMillis() - start) + " ms");

        if (kernel.getApplication().getDatabasePath("stels.db").exists()) {
            start = System.currentTimeMillis();
            ModelUpgrader.performUpgrade(this, kernel);
            Logger.d(TAG, "Database upgraded in " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    public ModelEngine getModel() {
        return model;
    }

    public void runKernel() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            model.markUnsentAsFailured();
        }
    }

    public void logIn() {
        clearData();
    }

    public void logOut() {
        clearData();
    }

    public void clearData() {
        model.dropData();
    }
}
