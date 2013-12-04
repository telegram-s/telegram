package org.telegram.android.kernel;

import org.telegram.android.core.*;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.cursors.ViewSource;
import org.telegram.android.log.Logger;

import java.util.HashMap;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class DataSourceKernel {

    private static final String TAG = "DataSourceKernel";

    private ApplicationKernel kernel;

    private DialogSource dialogSource;
    private HashMap<Long, MessageSource> messageSources = new HashMap<Long, MessageSource>();
    private UserSource userSource;
    private ContactsSource contactsSource;
    private ChatSource chatSource;
    private EncryptedChatSource encryptedChatSource;

    public DataSourceKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        init();
    }

    private void init() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            dialogSource = new DialogSource(kernel.getApplication());
            dialogSource.startSyncIfRequired();
            userSource = new UserSource(kernel.getApplication());
            contactsSource = new ContactsSource(kernel.getApplication());
            chatSource = new ChatSource(kernel.getApplication());
            encryptedChatSource = new EncryptedChatSource(kernel.getApplication());

            if (kernel.getStorageKernel().getModel().getDatabase().isWasUpgraded()) {
                dialogSource.resetSync();
                dialogSource.startSync();
            } else {
                dialogSource.startSyncIfRequired();
            }

            if (kernel.getTechKernel().getTechReflection().isAppUpgraded() || kernel.getStorageKernel().getModel().getDatabase().isWasUpgraded()) {
                contactsSource.resetState();
            }

            if (kernel.getStorageKernel().getModel().getDatabase().isWasUpgraded()) {
                MessageSource.clearData(kernel.getApplication());
            }

            contactsSource.startSync();
        }
    }

    public DialogSource getDialogSource() {
        return dialogSource;
    }

    public UserSource getUserSource() {
        return userSource;
    }

    public ContactsSource getContactsSource() {
        return contactsSource;
    }

    public ChatSource getChatSource() {
        return chatSource;
    }

    public EncryptedChatSource getEncryptedChatSource() {
        return encryptedChatSource;
    }


    public synchronized MessageSource getMessageSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            return messageSources.get(id);
        } else {
            MessageSource source = new MessageSource(peerType, peerId, kernel.getApplication());
            kernel.getUiKernel().getResponsibility().doPause(50);
            source.startSyncIfRequired();
            messageSources.put(id, source);
            return source;
        }
    }

    public synchronized void removeMessageSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            messageSources.remove(id);
        }
    }

    public synchronized ViewSource<ChatMessage> getMessagesViewSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            return messageSources.get(id).getMessagesSource();
        }

        return null;
    }

    public void onSourceAddMessage(ChatMessage message) {
        Logger.d(TAG, "find view source1");
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        Logger.d(TAG, "founded view source1");
        res.addItem(message);
    }

    public void onSourceAddMessageHacky(ChatMessage message) {
        Logger.d(TAG, "find view source2");
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        Logger.d(TAG, "founded view source2");
        res.addToEndHacky(message);
    }

    public void onSourceRemoveMessage(ChatMessage message) {
        Logger.d(TAG, "find view source3");
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        Logger.d(TAG, "founded view source3");
        res.removeItem(message);
    }

    public void onSourceUpdateMessage(ChatMessage message) {
        Logger.d(TAG, "find view source4");
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        Logger.d(TAG, "founded view source4");
        res.updateItem(message);
    }

    public synchronized void notifyUIUpdate() {
        Logger.w(TAG, "notifyUIUpdate");
        if (dialogSource != null && dialogSource.getViewSource() != null) {
            dialogSource.getViewSource().invalidateDataIfRequired();
        }
        if (messageSources != null) {
            for (MessageSource source : messageSources.values()) {
                if (source.getMessagesSource() != null) {
                    source.getMessagesSource().invalidateDataIfRequired();
                }
            }
        }
    }

    public void logIn() {
        dialogSource = new DialogSource(kernel.getApplication());
        userSource = new UserSource(kernel.getApplication());
        contactsSource = new ContactsSource(kernel.getApplication());
        chatSource = new ChatSource(kernel.getApplication());
        encryptedChatSource = new EncryptedChatSource(kernel.getApplication());

        dialogSource.resetSync();
        dialogSource.startSync();

        contactsSource.resetState();
        contactsSource.startSync();

        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();
        MessageSource.clearData(kernel.getApplication());
    }

    public void logOut() {
        // Clearing all messages states
        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();
        MessageSource.clearData(kernel.getApplication());

        // Clearing dialogs states
        dialogSource.destroy();
        DialogSource.clearData(kernel.getApplication());

        // Clearing contacts states
        contactsSource.destroy();
        ContactsSource.clearData(kernel.getApplication());

        chatSource.clear();
    }
}
