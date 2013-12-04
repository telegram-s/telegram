package org.telegram.android.kernel;

import org.telegram.android.core.DynamicConfig;
import org.telegram.android.core.TypingStates;
import org.telegram.android.core.background.MessageSender;
import org.telegram.android.core.background.UpdateProcessor;
import org.telegram.android.core.background.sync.BackgroundSync;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class SyncKernel {
    private ApplicationKernel kernel;

    private BackgroundSync backgroundSync;
    private UpdateProcessor updateProcessor;
    private MessageSender messageSender;
    private TypingStates typingStates;

    private DynamicConfig dynamicConfig;

    public SyncKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        init();
    }

    public UpdateProcessor getUpdateProcessor() {
        return updateProcessor;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public TypingStates getTypingStates() {
        return typingStates;
    }

    public DynamicConfig getDynamicConfig() {
        return dynamicConfig;
    }

    public BackgroundSync getBackgroundSync() {
        return backgroundSync;
    }

    private void init() {
        messageSender = new MessageSender(kernel.getApplication());
        typingStates = new TypingStates(kernel.getApplication());
        // updateProcessor = new UpdateProcessor(kernel.getApplication());
        dynamicConfig = new DynamicConfig(kernel.getApplication());
        backgroundSync = new BackgroundSync(kernel.getApplication());
    }

    public void runKernel() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            updateProcessor = new UpdateProcessor(kernel.getApplication());
            updateProcessor.invalidateUpdates();
            updateProcessor.runUpdateProcessor();
            // actions.checkHistory();
        }
        backgroundSync.run();
    }

    public void logIn() {
        typingStates.clearState();
        updateProcessor = new UpdateProcessor(kernel.getApplication());
        updateProcessor.clearData();
        updateProcessor.invalidateUpdates();
        updateProcessor.runUpdateProcessor();
    }

    public void logOut() {
        typingStates.clearState();
        updateProcessor.destroy();
        updateProcessor.clearData();
        updateProcessor = null;
    }
}
