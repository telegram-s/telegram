package org.telegram.android.kernel;

import org.telegram.android.core.DynamicConfig;
import org.telegram.android.core.TypingStates;
import org.telegram.android.core.background.FastActions;
import org.telegram.android.core.background.MessageSender;
import org.telegram.android.core.background.TechSyncer;
import org.telegram.android.core.background.UpdateProcessor;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class SyncKernel {
    private ApplicationKernel kernel;

    private UpdateProcessor updateProcessor;
    private MessageSender messageSender;
    private FastActions actions;
    private TypingStates typingStates;

    private TechSyncer techSyncer;
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

    public FastActions getActions() {
        return actions;
    }

    public TypingStates getTypingStates() {
        return typingStates;
    }

    public TechSyncer getTechSyncer() {
        return techSyncer;
    }

    public DynamicConfig getDynamicConfig() {
        return dynamicConfig;
    }

    private void init() {
        messageSender = new MessageSender(kernel.getApplication());
        actions = new FastActions(kernel.getApplication());
        typingStates = new TypingStates(kernel.getApplication());
        // updateProcessor = new UpdateProcessor(kernel.getApplication());
        techSyncer = new TechSyncer(kernel.getApplication());
        dynamicConfig = new DynamicConfig(kernel.getApplication());
    }

    public void runKernel() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            updateProcessor = new UpdateProcessor(kernel.getApplication());
            updateProcessor.invalidateUpdates();
            updateProcessor.runUpdateProcessor();
            actions.checkForDeletions();
            actions.checkHistory();
            techSyncer.onLogin();
        }
        techSyncer.checkDC();
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
        updateProcessor.clearData();
        updateProcessor.destroy();
        updateProcessor = null;
    }
}
