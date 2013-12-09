package org.telegram.android.kernel;

import org.telegram.android.core.TypingStates;
import org.telegram.android.core.background.MessageSender;
import org.telegram.android.core.background.UpdateProcessor;
import org.telegram.android.core.background.sync.BackgroundSync;
import org.telegram.android.core.background.sync.ContactsSync;
import org.telegram.android.log.Logger;

import java.util.Locale;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class SyncKernel {

    private static final String TAG = "SyncKernel";

    private ApplicationKernel kernel;

    private ContactsSync contactsSync;
    private BackgroundSync backgroundSync;
    private UpdateProcessor updateProcessor;
    private MessageSender messageSender;
    private TypingStates typingStates;

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

    public BackgroundSync getBackgroundSync() {
        return backgroundSync;
    }

    public ContactsSync getContactsSync() {
        return contactsSync;
    }

    private void init() {
        long start = System.currentTimeMillis();
        messageSender = new MessageSender(kernel.getApplication());
        Logger.d(TAG, "MessageSender loaded in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        typingStates = new TypingStates(kernel.getApplication());
        Logger.d(TAG, "TypingStates loaded in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        backgroundSync = new BackgroundSync(kernel.getApplication());
        Logger.d(TAG, "BackgroundSync loaded in " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        contactsSync = new ContactsSync(kernel.getApplication());
        Logger.d(TAG, "ContactsSync loaded in " + (System.currentTimeMillis() - start) + " ms");
    }

    public void runKernel() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            updateProcessor = new UpdateProcessor(kernel.getApplication());
            updateProcessor.invalidateUpdates();
            updateProcessor.runUpdateProcessor();
            // actions.checkHistory();
        }
        backgroundSync.run();
        contactsSync.run();

        if (!Locale.getDefault().getLanguage().equals(kernel.getTechKernel().getSystemConfig().getInviteMessageLang())) {
            backgroundSync.resetInviteSync();
        }
    }

    public void logIn() {
        typingStates.clearState();
        updateProcessor = new UpdateProcessor(kernel.getApplication());
        updateProcessor.clearData();
        updateProcessor.invalidateUpdates();
        updateProcessor.runUpdateProcessor();
        contactsSync.invalidateContactsSync();
    }

    public void logOut() {
        typingStates.clearState();
        contactsSync.clear();
        contactsSync.resetSync();
        if (updateProcessor != null) {
            updateProcessor.destroy();
            updateProcessor.clearData();
            updateProcessor = null;
        }
    }
}
