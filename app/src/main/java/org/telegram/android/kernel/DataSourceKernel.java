package org.telegram.android.kernel;

import org.telegram.android.core.*;
import org.telegram.android.core.engines.SyncStateEngine;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.MediaRecord;
import org.telegram.android.core.wireframes.MessageWireframe;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class DataSourceKernel {

    private static final String TAG = "DataSourceKernel";

    private ApplicationKernel kernel;

    private volatile boolean wasInited = false;
    private volatile DialogSource dialogSource;
    private volatile HashMap<Long, MessageSource> messageSources = new HashMap<Long, MessageSource>();
    private volatile HashMap<Long, MediaSource> mediaSources = new HashMap<Long, MediaSource>();
    private volatile UserSource userSource;
    private volatile ContactsSource contactsSource;
    private volatile ChatSource chatSource;
    private volatile EncryptedChatSource encryptedChatSource;
    private volatile WebSearchSource webSearchSource;

    public DataSourceKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        init();
    }

    private void init() {
        webSearchSource = new WebSearchSource();
        if (kernel.getAuthKernel().isLoggedIn()) {
            dialogSource = new DialogSource(kernel.getApplication());
            userSource = new UserSource();
            contactsSource = new ContactsSource(kernel.getApplication());
            chatSource = new ChatSource(kernel.getApplication());
            encryptedChatSource = new EncryptedChatSource(kernel.getApplication());
            wasInited = true;
        }
    }

    public void runKernel() {
        if (kernel.getAuthKernel().isLoggedIn()) {
            contactsSource.run();
            dialogSource.startSyncIfRequired();
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

    public WebSearchSource getWebSearchSource() {
        return webSearchSource;
    }

    public synchronized MediaSource getMediaSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (mediaSources.containsKey(id)) {
            return mediaSources.get(id);
        } else {
            MediaSource source = new MediaSource(peerType, peerId, kernel.getApplication());
            kernel.getUiKernel().getResponsibility().doPause(50);
            source.startSyncIfRequired();
            mediaSources.put(id, source);
            return source;
        }
    }

    public void onSourceAddMedia(MediaRecord record) {
        ViewSource<MediaRecord, MediaRecord> res = getMediaSource(record.getPeerType(), record.getPeerId()).getSource();
        if (res == null)
            return;
        res.addItem(record);
    }

    public void onSourceRemoveMedia(MediaRecord record) {
        ViewSource<MediaRecord, MediaRecord> res = getMediaSource(record.getPeerType(), record.getPeerId()).getSource();
        if (res == null)
            return;
        res.removeItem(record);
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

    public synchronized ViewSource<MessageWireframe, ChatMessage> getMessagesViewSource(int peerType, int peerId) {
        long id = peerType + peerId * 10;
        if (messageSources.containsKey(id)) {
            return messageSources.get(id).getMessagesSource();
        }

        return null;
    }

    public void onSourceAddMessage(ChatMessage message) {
        ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addItem(message);
    }

    public void onSourceAddMessages(ChatMessage[] message) {
        HashSet<Long> ids = new HashSet<Long>();
        for (ChatMessage msg : message) {
            ids.add(msg.getPeerId() * 10L + msg.getPeerType());
        }

        for (Long id : ids) {
            int peerType = (int) ((10 + id % 10L) % 10);
            int peerId = (int) ((id - peerType) / 10L);

            ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(peerType, peerId);
            if (res == null)
                continue;

            ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();
            for (ChatMessage msg : message) {
                if (msg.getPeerId() == peerId && msg.getPeerType() == peerType) {
                    msgs.add(msg);
                }
            }
            res.addItems(msgs.toArray(new ChatMessage[msgs.size()]));
        }

    }

    public void onSourceUpdateMessages(ChatMessage[] message) {
        HashSet<Long> ids = new HashSet<Long>();
        for (ChatMessage msg : message) {
            ids.add(msg.getPeerId() * 10L + msg.getPeerType());
        }

        for (Long id : ids) {
            int peerType = (int) ((10 + id % 10L) % 10);
            int peerId = (int) ((id - peerType) / 10L);

            ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(peerType, peerId);
            if (res == null)
                continue;

            ArrayList<ChatMessage> msgs = new ArrayList<ChatMessage>();
            for (ChatMessage msg : message) {
                if (msg.getPeerId() == peerId && msg.getPeerType() == peerType) {
                    msgs.add(msg);
                }
            }
            res.updateItems(msgs.toArray(new ChatMessage[msgs.size()]));
        }

    }

    public void onSourceAddMessageHacky(ChatMessage message) {
        ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.addToEndHacky(message);
    }

    public void onSourceRemoveMessage(ChatMessage message) {
        ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
        res.removeItem(message);
    }

    public void onSourceUpdateMessage(ChatMessage message) {
        ViewSource<MessageWireframe, ChatMessage> res = getMessagesViewSource(message.getPeerType(), message.getPeerId());
        if (res == null)
            return;
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
        if (mediaSources != null) {
            for (MediaSource source : mediaSources.values()) {
                if (source.getSource() != null) {
                    source.getSource().invalidateDataIfRequired();
                }
            }
        }
    }

    public boolean isWasInited() {
        return wasInited;
    }

    public void logIn() {
        dialogSource = new DialogSource(kernel.getApplication());
        userSource = new UserSource();
        contactsSource = new ContactsSource(kernel.getApplication());
        chatSource = new ChatSource(kernel.getApplication());
        encryptedChatSource = new EncryptedChatSource(kernel.getApplication());

        dialogSource.resetSync();
        dialogSource.startSync();

        // contactsSource.resetState();

        contactsSource.run();

        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();

        wasInited = true;
    }

    public void logOut() {
        // Clearing all messages states
        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();

        // Clearing dialogs states
        dialogSource.destroy();

        // Clearing contacts states
        contactsSource.destroy();
        // ContactsSource.clearData(kernel.getApplication());

        chatSource.clear();
    }

    public void onFontChanged() {
        for (MessageSource source : messageSources.values()) {
            source.destroy();
        }
        messageSources.clear();

        if (dialogSource != null) {
            dialogSource.destroy();
        }
        dialogSource = new DialogSource(kernel.getApplication());
    }
}
