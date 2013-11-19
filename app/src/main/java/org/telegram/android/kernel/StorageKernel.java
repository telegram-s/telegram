package org.telegram.android.kernel;

import org.telegram.android.core.ModelEngine;
import org.telegram.android.core.StelsDatabase;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class StorageKernel {
    private ApplicationKernel kernel;
    private StelsDatabase database;
    private ModelEngine model;

    public StorageKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        this.database = new StelsDatabase(kernel.getApplication());
        this.model = new ModelEngine(database, kernel.getApplication());
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
