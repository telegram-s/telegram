package org.telegram.android.kernel;

import org.telegram.android.core.EncryptionController;
import org.telegram.android.core.background.EncryptedChatProcessor;
import org.telegram.android.core.background.SelfDestructProcessor;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class EncryptedKernel {
    private ApplicationKernel kernel;
    private EncryptionController encryptionController;
    private SelfDestructProcessor selfDestructProcessor;
    private EncryptedChatProcessor encryptedChatProcessor;

    public EncryptedKernel(ApplicationKernel kernel) {
        this.kernel = kernel;

        encryptedChatProcessor = new EncryptedChatProcessor(kernel.getApplication());
        encryptionController = new EncryptionController(kernel.getApplication());
        selfDestructProcessor = new SelfDestructProcessor(kernel.getApplication());

        if (kernel.getAuthKernel().isLoggedIn()) {
            selfDestructProcessor.checkInitialDeletions();
        }
    }

    public EncryptionController getEncryptionController() {
        return encryptionController;
    }

    public SelfDestructProcessor getSelfDestructProcessor() {
        return selfDestructProcessor;
    }

    public EncryptedChatProcessor getEncryptedChatProcessor() {
        return encryptedChatProcessor;
    }
}
