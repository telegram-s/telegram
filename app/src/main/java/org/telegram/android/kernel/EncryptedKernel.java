package org.telegram.android.kernel;

import org.telegram.android.core.EncryptionController;
import org.telegram.android.core.background.SelfDestructProcessor;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class EncryptedKernel {
    private static final String TAG = "EncryptedKernel";
    private ApplicationKernel kernel;
    private EncryptionController encryptionController;
    private SelfDestructProcessor selfDestructProcessor;

    public EncryptedKernel(ApplicationKernel kernel) {
        this.kernel = kernel;

        long start = System.currentTimeMillis();
        encryptionController = new EncryptionController(kernel.getApplication());
        Logger.d(TAG, "EncryptionController loaded in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        selfDestructProcessor = new SelfDestructProcessor(kernel.getApplication());
        Logger.d(TAG, "SelfDestructProcessor loaded in " + (System.currentTimeMillis() - start) + " ms");
    }

    public EncryptionController getEncryptionController() {
        return encryptionController;
    }

    public SelfDestructProcessor getSelfDestructProcessor() {
        return selfDestructProcessor;
    }

    public void runKernel() {
        encryptionController.run();
        selfDestructProcessor.runProcessor();

        if (kernel.getAuthKernel().isLoggedIn()) {
            long start = System.currentTimeMillis();
            selfDestructProcessor.requestInitialDeletions();
            Logger.d(TAG, "SelfDestructProcessor checkInitialDeletions in " + (System.currentTimeMillis() - start) + " ms");
        }
    }
}
