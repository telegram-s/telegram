package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v4.text.BidiFormatter;
import org.telegram.android.Configuration;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.engines.SyncStateEngine;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.service.*;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.ui.source.ViewSource;
import org.telegram.android.ui.source.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.messages.TLAbsDialogs;
import org.telegram.api.messages.TLDialogs;
import org.telegram.api.messages.TLDialogsSlice;
import org.telegram.api.requests.TLRequestMessagesGetDialogs;
import org.telegram.tl.TLObject;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 21:22
 */
public class DialogSource {
    private static final String TAG = "DialogSource";

    private static final int STATE_UNLOADED = 0;
    private static final int STATE_LOADED = 1;
    private static final int STATE_COMPLETED = 2;

    public static final int PAGE_SIZE = 40;
    public static final int PAGE_SIZE_REMOTE = 20;

    public static final int PAGE_OVERLAP = 3;

    public static final int PAGE_REQUEST_PADDING = 5;

    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("DialogSourceService#" + res.hashCode());
            res.setPriority(Configuration.SOURCES_THREAD_PRIORITY);
            return res;
        }
    });

    private final Object stateSync = new Object();
    private DialogSourceState state;
    private int persistenceState = STATE_UNLOADED;

    private StelsApplication application;

    private ViewSource<DialogWireframe, DialogDescription> dialogsSource;

    private boolean isDestroyed;
    private SyncStateEngine syncStateEngine;

    public DialogSource(StelsApplication _application) {
        this.application = _application;
        this.isDestroyed = false;
        this.syncStateEngine = application.getEngine().getSyncStateEngine();

        this.persistenceState = syncStateEngine.getDialogsSyncState(STATE_UNLOADED);

        if (persistenceState == STATE_COMPLETED) {
            this.state = DialogSourceState.COMPLETED;
        } else if (persistenceState == STATE_LOADED) {
            this.state = DialogSourceState.SYNCED;
        } else {
            this.state = DialogSourceState.UNSYNCED;
        }

        this.dialogsSource = new ViewSource<DialogWireframe, DialogDescription>(true) {
            @Override
            protected DialogDescription[] loadItems(int offset) {

                if (offset < PAGE_OVERLAP) {
                    offset = 0;
                } else {
                    offset -= PAGE_OVERLAP;
                }

                long start = SystemClock.uptimeMillis();
                DialogDescription[] res = application.getEngine().getDialogsEngine().getItems(offset, PAGE_SIZE);
                Logger.d(TAG, "Items loaded in " + (SystemClock.uptimeMillis() - start) + " ms");

                HashSet<Integer> users = new HashSet<Integer>();
                HashSet<Integer> secretChats = new HashSet<Integer>();
                HashSet<Integer> groups = new HashSet<Integer>();
                for (DialogDescription description : res) {
                    if (description.getSenderId() > 0) {
                        users.add(description.getSenderId());
                    }

                    if (description.getPeerType() == PeerType.PEER_USER) {
                        users.add(description.getPeerId());
                    } else if (description.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                        secretChats.add(description.getPeerId());
                    } else if (description.getPeerType() == PeerType.PEER_CHAT) {
                        groups.add(description.getPeerId());
                    }
                }

                start = SystemClock.uptimeMillis();
                application.getEngine().getGroupsEngine().getGroups(groups.toArray(new Integer[0]));
                Logger.d(TAG, "Groups loaded in " + (SystemClock.uptimeMillis() - start) + " ms");
                start = SystemClock.uptimeMillis();
                EncryptedChat[] chats = application.getEngine().getSecretEngine().getEncryptedChats(secretChats.toArray(new Integer[0]));
                for (int i = 0; i < chats.length; i++) {
                    users.add(chats[i].getUserId());
                }
                Logger.d(TAG, "Secret chats loaded in " + (SystemClock.uptimeMillis() - start) + " ms");
                start = SystemClock.uptimeMillis();
                application.getEngine().getUsersEngine().getUsersById(users.toArray());
                Logger.d(TAG, "Users loaded in " + (SystemClock.uptimeMillis() - start) + " ms");
                return res;
            }

            @Override
            protected long getSortingKey(DialogWireframe obj) {
                return obj.getDate() * 1000L + Math.abs(obj.getMid()) % 1000;
            }

            @Override
            protected long getItemKey(DialogWireframe obj) {
                return obj.getDatabaseId();
            }

            @Override
            protected long getItemKeyV(DialogDescription obj) {
                return obj.getUniqId();
            }

            @Override
            protected ViewSourceState getInternalState() {
                switch (state) {
                    default:
                    case UNSYNCED:
                        return ViewSourceState.UNSYNCED;
                    case COMPLETED:
                        return ViewSourceState.COMPLETED;
                    case LOAD_MORE_ERROR:
                        return ViewSourceState.LOAD_MORE_ERROR;
                    case LOAD_MORE:
                        return ViewSourceState.IN_PROGRESS;
                    case FULL_SYNC:
                        return ViewSourceState.IN_PROGRESS;
                    case SYNCED:
                        return ViewSourceState.SYNCED;
                }
            }

            @Override
            protected void onItemRequested(int index) {
                if (index > getItemsCount() - PAGE_REQUEST_PADDING) {
                    requestLoadMore(getItemsCount());
                }
            }

            @Override
            protected DialogWireframe convert(DialogDescription item) {
                return DialogSource.this.convert(item);
            }
        };
    }

    public ViewSource<DialogWireframe, DialogDescription> getViewSource() {
        return dialogsSource;
    }

    public DialogSourceState getState() {
        return state;
    }

    private void setState(DialogSourceState nState) {
        if (isDestroyed)
            return;
        state = nState;
        dialogsSource.invalidateState();
    }

    private void onCompleted() {
        if (isDestroyed)
            return;
        persistenceState = STATE_COMPLETED;
        syncStateEngine.setDialogsSyncState(persistenceState);
    }

    private void onLoaded() {
        if (isDestroyed)
            return;
        persistenceState = STATE_LOADED;
        syncStateEngine.setDialogsSyncState(persistenceState);
    }

    public void startSyncIfRequired() {
        if (state == DialogSourceState.UNSYNCED) {
            startSync();
        }
    }

    public void startSync() {
        if (isDestroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != DialogSourceState.FULL_SYNC && state != DialogSourceState.LOAD_MORE) {
                setState(DialogSourceState.FULL_SYNC);
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            boolean isCompleted = false;
                            try {
                                while (application.isLoggedIn()) {
                                    try {
                                        isCompleted = doLoadDialogs(0);
                                        dialogsSource.invalidateDataIfRequired();
                                        // notifyDataChanged();
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                            } finally {
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.FULL_SYNC) {
                                        if (isCompleted) {
                                            setState(DialogSourceState.COMPLETED);
                                            onCompleted();
                                        } else {
                                            setState(DialogSourceState.SYNCED);
                                            onLoaded();
                                        }
                                    }
                                }
                            }

                            setState(DialogSourceState.UNSYNCED);
                        } finally {
                            Logger.d(TAG, "Dialogs full sync time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }

    public void resetSync() {
        setState(DialogSourceState.UNSYNCED);
    }

    public void requestLoadMore(final int offset) {
        if (isDestroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != DialogSourceState.FULL_SYNC && state != DialogSourceState.LOAD_MORE
                    && state != DialogSourceState.COMPLETED) {
                setState(DialogSourceState.LOAD_MORE);
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            try {
                                boolean isCompleted = doLoadDialogs(offset);
                                dialogsSource.invalidateDataIfRequired();
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.LOAD_MORE) {
                                        if (isCompleted) {
                                            setState(DialogSourceState.COMPLETED);
                                            onCompleted();
                                        } else {
                                            setState(DialogSourceState.SYNCED);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.LOAD_MORE) {
                                        setState(DialogSourceState.LOAD_MORE_ERROR);
                                    }
                                }
                            }
                        } finally {
                            Logger.d(TAG, "Dialogs load more time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }


    public boolean isCompleted() {
        return state == DialogSourceState.COMPLETED;
    }

    public void destroy() {
        isDestroyed = true;
        service.shutdownNow();
    }


    private boolean doLoadDialogs(int offset) throws Exception {
        if (isDestroyed) {
            return true;
        }
        application.getMonitor().waitForConnection();
        if (isDestroyed) {
            return true;
        }
        TLAbsDialogs dialogs = application.getApi().doRpcCall(new TLRequestMessagesGetDialogs(offset, 0, PAGE_SIZE_REMOTE));
        if (isDestroyed) {
            return true;
        }

        long start = SystemClock.uptimeMillis();
        application.getEngine().onUsers(dialogs.getUsers());
        application.getEngine().getGroupsEngine().onGroupsUpdated(dialogs.getChats());
        application.getEngine().onLoadMoreDialogs(dialogs.getMessages(), dialogs.getDialogs());
        Logger.d(TAG, "Dialog apply time: " + (SystemClock.uptimeMillis() - start) + " ms");

        if (dialogs instanceof TLDialogs) {
            return true;
        } else if (dialogs instanceof TLDialogsSlice) {
            return dialogs.getMessages().size() == 0 || ((TLDialogsSlice) dialogs).getCount() <= offset + PAGE_SIZE_REMOTE;
        } else {
            return true;
        }
    }

    protected String getString(int id) {
        return application.getString(id);
    }

    protected DialogWireframe convert(DialogDescription item) {
        DialogWireframe res = new DialogWireframe(item.getUniqId(), item.getPeerId(), item.getPeerType());

        res.setMid(item.getTopMessageId());
        res.setSenderId(item.getSenderId());
        res.setMine(res.getSenderId() == application.getCurrentUid());
        res.setSender(application.getEngine().getUser(item.getSenderId()));

        if (res.getPeerType() == PeerType.PEER_USER) {
            res.setDialogUser(application.getEngine().getUser(res.getPeerId()));
        } else if (res.getPeerType() == PeerType.PEER_CHAT) {
            res.setDialogGroup(application.getEngine().getGroupsEngine().getGroup(res.getPeerId()));
        } else if (res.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat encryptedChat = application.getEngine().getEncryptedChat(res.getPeerId());
            res.setDialogUser(application.getEngine().getUser(encryptedChat.getUserId()));
        } else {
            throw new RuntimeException("Unknown peer type: " + res.getPeerType());
        }

        res.setDate(item.getDate());
        res.setMessageState(item.getMessageState());
        res.setContentType(item.getRawContentType());

        if (res.getContentType() == ContentType.MESSAGE_TEXT) {
            String rawMessage = item.getMessage();
            if (rawMessage.length() > 50) {
                rawMessage = rawMessage.substring(0, 50) + "...";
            }
            String[] rows = rawMessage.split("\n", 2);
            if (rows.length == 2) {
                rawMessage = rows[0];
            }
            res.setMessage(rawMessage);
        } else if (res.getContentType() == ContentType.MESSAGE_SYSTEM) {
            String body;
            TLObject object = item.getExtras();
            if (object != null && object instanceof TLAbsLocalAction) {
                boolean isMyself = res.isMine();
                String senderName = res.isMine() ? getString(R.string.st_dialog_you) : res.getSender() != null ? res.getSender().getFirstName() : "???";
                if (object instanceof TLLocalActionChatCreate) {
                    body = getString(isMyself ? R.string.st_dialog_created_group_you : R.string.st_dialog_created_group)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionChatDeleteUser) {
                    int uid = ((TLLocalActionChatDeleteUser) object).getUserId();
                    if (uid == res.getSenderId()) {
                        body = getString(isMyself ? R.string.st_dialog_left_user_you : R.string.st_dialog_left_user)
                                .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                    } else {
                        if (uid == application.getCurrentUid()) {
                            body = getString(R.string.st_dialog_kicked_user_of_you).replace("{0}", getString(R.string.st_dialog_you_r))
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        } else {
                            User usr = application.getEngine().getUserRuntime(uid);
                            body = getString(isMyself ? R.string.st_dialog_kicked_user_you : R.string.st_dialog_kicked_user).replace("{0}", usr.getFirstName())
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        }
                    }
                } else if (object instanceof TLLocalActionChatAddUser) {
                    int uid = ((TLLocalActionChatAddUser) object).getUserId();
                    if (uid == res.getSenderId()) {
                        body = getString(isMyself ? R.string.st_dialog_enter_user_you : R.string.st_dialog_enter_user)
                                .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                    } else {
                        if (uid == application.getCurrentUid()) {
                            body = getString(R.string.st_dialog_added_user_of_you).replace("{0}", getString(R.string.st_dialog_you_r))
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        } else {
                            User usr = application.getEngine().getUserRuntime(uid);
                            body = getString(isMyself ? R.string.st_dialog_added_user_you : R.string.st_dialog_added_user).replace("{0}", usr.getFirstName())
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        }
                    }
                } else if (object instanceof TLLocalActionChatDeletePhoto) {
                    body = getString(isMyself ? R.string.st_dialog_removed_photo_you : R.string.st_dialog_removed_photo)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionChatEditPhoto) {
                    body = getString(isMyself ? R.string.st_dialog_changed_photo_you : R.string.st_dialog_changed_photo)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionChatEditTitle) {
                    body = getString(isMyself ? R.string.st_dialog_changed_name_you : R.string.st_dialog_changed_name)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionUserRegistered) {
                    body = getString(R.string.st_dialog_user_joined_app)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionUserEditPhoto) {
                    body = getString(R.string.st_dialog_user_add_avatar)
                            .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                } else if (object instanceof TLLocalActionEncryptedTtl) {
                    TLLocalActionEncryptedTtl ttl = (TLLocalActionEncryptedTtl) object;
                    if (res.isMine()) {
                        if (ttl.getTtlSeconds() > 0) {
                            body = getString(R.string.st_dialog_encrypted_switched_you).replace(
                                    "{time}", BidiFormatter.getInstance().unicodeWrap(TextUtil.formatHumanReadableDuration(ttl.getTtlSeconds())))
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        } else {
                            body = getString(R.string.st_dialog_encrypted_switched_off_you)
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        }
                    } else {
                        if (ttl.getTtlSeconds() > 0) {
                            body = getString(R.string.st_dialog_encrypted_switched).replace(
                                    "{time}", TextUtil.formatHumanReadableDuration(ttl.getTtlSeconds()))
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        } else {
                            body = getString(R.string.st_dialog_encrypted_switched_off)
                                    .replace("{sender}", BidiFormatter.getInstance().unicodeWrap(senderName));
                        }
                    }
                } else if (object instanceof TLLocalActionEncryptedCancelled) {
                    body = getString(R.string.st_dialog_encrypted_cancelled);
                } else if (object instanceof TLLocalActionEncryptedRequested) {
                    body = getString(R.string.st_dialog_encrypted_requested);
                } else if (object instanceof TLLocalActionEncryptedWaiting) {
                    body = getString(R.string.st_dialog_encrypted_waiting)
                            .replace("{name}", res.getDialogUser().getFirstName());
                } else if (object instanceof TLLocalActionEncryptedCreated) {
                    body = getString(R.string.st_dialog_encrypted_created);
                } else if (object instanceof TLLocalActionEncryptedMessageDestructed) {
                    body = getString(R.string.st_dialog_encrypted_selfdestructed);
                } else {
                    body = getString(R.string.st_dialog_system);
                }
            } else {
                body = getString(R.string.st_dialog_system);
            }
            res.setMessage(body);
        } else {
            String body;
            switch (res.getContentType()) {
                case ContentType.MESSAGE_VIDEO:
                    body = getString(R.string.st_dialog_video);
                    break;
                case ContentType.MESSAGE_GEO:
                    body = getString(R.string.st_dialog_geo);
                    break;
                case ContentType.MESSAGE_PHOTO:
                    body = getString(R.string.st_dialog_photo);
                    break;
                case ContentType.MESSAGE_CONTACT:
                    body = getString(R.string.st_dialog_contact);
                    break;
                case ContentType.MESSAGE_AUDIO:
                    body = getString(R.string.st_dialog_audio);
                    break;
                case ContentType.MESSAGE_DOC_PREVIEW:
                case ContentType.MESSAGE_DOCUMENT:
                    body = getString(R.string.st_dialog_document);
                    break;
                case ContentType.MESSAGE_DOC_ANIMATED:
                    body = getString(R.string.st_dialog_animation);
                    break;
                default:
                    body = getString(R.string.st_dialog_unknown);
                    break;
            }
            res.setMessage(body);
        }

        res.setUnreadCount(item.getUnreadCount());

        // res.setPreparedLayout(DialogView.prepareLayoutCache(res, application));

        return res;
    }
}
