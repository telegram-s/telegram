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
        dialogSource = new DialogSource(kernel.getApplication());
        userSource = new UserSource(kernel.getApplication());
        contactsSource = new ContactsSource(kernel.getApplication());
        chatSource = new ChatSource(kernel.getApplication());
        encryptedChatSource = new EncryptedChatSource(kernel.getApplication());
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
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addItem(message);
    }

    public void onSourceAddMessageHacky(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addToEndHacky(message);
    }

    public void onSourceRemoveMessage(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.removeItem(message);
    }

    public void onSourceUpdateMessage(ChatMessage message) {
        ViewSource<ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.updateItem(message);
    }

    public synchronized void notifyUIUpdate() {
        Logger.w(TAG, "notifyUIUpdate");
        long start = System.currentTimeMillis();
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
        Logger.w(TAG, "notifyUIUpdate: " + (System.currentTimeMillis() - start) + " ms");
    }
}
