package org.telegram.android.kernel;

import org.telegram.android.core.ModelEngine;
import org.telegram.android.core.StelsDatabase;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class StorageKernel {
    private static final String TAG = "StorageKernel";

    private ApplicationKernel kernel;
    private StelsDatabase database;
    private ModelEngine model;

    public StorageKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        long start = System.currentTimeMillis();
        this.database = new StelsDatabase(kernel.getApplication());
        Logger.d(TAG, "Database loaded in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        this.model = new ModelEngine(database, kernel.getApplication());
        Logger.d(TAG, "ModelEngine loaded in " + (System.currentTimeMillis() - start) + " ms");
    }

    public StelsDatabase getDatabase() {
        return database;
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
        database.clearData();
        model.clearCache();
    }
}
