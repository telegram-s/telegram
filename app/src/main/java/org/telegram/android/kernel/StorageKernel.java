package org.telegram.android.kernel;

import org.telegram.android.core.engines.ModelEngine;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class StorageKernel {
    private static final String TAG = "StorageKernel";

    private ApplicationKernel kernel;
    private ModelEngine model;

    public StorageKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        long start = System.currentTimeMillis();
        this.model = new ModelEngine(kernel.getApplication());
        Logger.d(TAG, "ModelEngine loaded in " + (System.currentTimeMillis() - start) + " ms");
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
