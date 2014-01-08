package org.telegram.android.core.engines;

import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.service.TLLocalActionChatDeleteUser;
import org.telegram.android.core.model.service.TLLocalActionEncryptedMessageDestructed;
import org.telegram.android.log.Logger;
import org.telegram.api.TLDialog;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class DialogsEngine {
    private DialogsDatabase database;
    private StelsApplication application;
    private ModelEngine engine;

    public DialogsEngine(ModelEngine engine) {
        this.engine = engine;
        this.application = engine.getApplication();
        this.database = new DialogsDatabase(engine);
    }

    public synchronized DialogDescription[] getItems(int offset, int limit) {
        return database.getItems(offset, limit);
    }

    public synchronized DialogDescription[] getAll() {
        return database.getAll();
    }

    public synchronized DialogDescription[] getUnreadedRemotelyDescriptions() {
        return database.getUnreadedRemotelyDescriptions();
    }

    public synchronized DialogDescription loadDialog(int peerType, int peerId) {
        return database.loadDialog(peerType, peerId);
    }

    public synchronized DialogDescription[] loadDialogs(Long[] uniqIds) {
        return database.loadDialogs(uniqIds);
    }

    public synchronized void updateOrCreateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public synchronized void updateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public synchronized void updateDialogs(DialogDescription[] description) {
        database.updateOrCreateDialogs(description);
        for (DialogDescription d : description) {
            application.getDialogSource().getViewSource().updateItem(d);
        }
    }

    public synchronized void updateOrCreateDialog(DialogDescription[] description) {
        database.updateOrCreateDialogs(description);
        for (DialogDescription d : description) {
            application.getDialogSource().getViewSource().updateItem(d);
        }
    }

    public synchronized void deleteDialog(int peerType, int peerId) {
        database.deleteDialog(peerType, peerId);
        application.getDialogSource().getViewSource().removeItemByKey(peerType * 10L + peerId);
    }

    public synchronized void markDialogAsNonFailed(int peerType, int peerId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setFailure(false);
            updateDialog(description);
        }
    }

    public synchronized void applyDescriptor(DialogDescription description, ChatMessage msg) {
        if (msg.getState() == MessageState.PENDING || msg.getState() == MessageState.FAILURE) {
            description.setTopMessageId(-msg.getDatabaseId());
        } else {
            description.setTopMessageId(msg.getMid());
        }
        description.setMessage(msg.getMessage());
        description.setDate(msg.getDate());
        description.setContentType(msg.getContentType());
        if (msg.isOut()) {
            description.setMessageState(msg.getState());
        } else {
            description.setMessageState(-1);
        }
        description.setSenderId(msg.getSenderId());
        description.setExtras(msg.getExtras());
    }

    public synchronized void updateDescriptorPending(ChatMessage msg) {
        DialogDescription description = loadDialog(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            applyDescriptor(description, msg);
            updateDialog(description);
        } else {
            if (msg.getPeerType() == PeerType.PEER_USER) {
                description = new DialogDescription(PeerType.PEER_USER, msg.getPeerId());
                applyDescriptor(description, msg);
                updateDialog(description);
            }
        }
    }

    public synchronized void updateDescriptorShort(ChatMessage msg) {
        DialogDescription description = loadDialog(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getDate() <= msg.getDate()) {
                applyDescriptor(description, msg);
                updateDialog(description);
            }
        } else {
            if (msg.getPeerType() == PeerType.PEER_USER) {
                description = new DialogDescription(PeerType.PEER_USER, msg.getPeerId());
                applyDescriptor(description, msg);
                updateOrCreateDialog(description);
            }
        }
    }

    public synchronized void updateDescriptorShortEnc(ChatMessage msg) {
        DialogDescription description = loadDialog(msg.getPeerType(), msg.getPeerId());
        if (description != null) {
            if (description.getDate() <= msg.getDate()) {
                if (description.getFirstUnreadMessage() == 0) {
                    description.setFirstUnreadMessage(msg.getRandomId());
                }

                applyDescriptor(description, msg);
                updateDialog(description);
            }
        }
    }

    public synchronized void updateDescriptorDeleteUnsent(int peerType, int peerId, int localId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -localId) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    public synchronized void updateDescriptorDeleteSent(int peerType, int peerId, int mid) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == mid) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    public synchronized void updateDescriptorSelfDestructed(int peerType, int peerId, int mid) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == mid) {
                description.setMessageState(MessageState.READED);
                description.setContentType(ContentType.MESSAGE_SYSTEM);
                description.setMessage("");
                description.setExtras(new TLLocalActionEncryptedMessageDestructed());
                updateDialog(description);
            }
        }
    }

    public synchronized void onMaxLocalViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setLastLocalViewedMessage(maxId);
            description.setUnreadCount(0);
            updateDialog(description);
        }
    }

    public synchronized void clearFirstUnreadMessage(int peerType, int peerId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setFirstUnreadMessage(0);
            description.setUnreadCount(0);
            updateDialog(description);
        }
    }

    public synchronized void onMaxRemoteViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setLastRemoteViewedMessage(maxId);
            updateDialog(description);
        }
    }

    public synchronized void updateDescriptorEncSent(int peerType, int peerId, int date, int databaseId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -databaseId) {
                description.setMessageState(MessageState.SENT);
                description.setDate(date);
                updateDialog(description);
            }
        }
    }

    public synchronized void updateDescriptorSent(int peerType, int peerId, int date, int mid, int databaseId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -databaseId) {
                description.setMessageState(MessageState.SENT);
                description.setTopMessageId(mid);
                description.setDate(date);
                updateDialog(description);
            }
        }
    }

    private synchronized void updateDescriptorFromScratch(int peerType, int peerId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            try {
                ChatMessage msg = engine.getMessagesEngine().findTopMessage(peerType, peerId);
                if (msg == null) {
                    description.setDate(0);
                    description.setTopMessageId(0);
                    description.setMessage("");
                    description.setContentType(0);
                    description.setSenderId(0);
                    updateDialog(description);
                } else {
                    applyDescriptor(description, msg);
                    updateDialog(description);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void updateDescriptors(ChatMessage[] res, List<TLDialog> dialogs) {
        HashSet<Long> dialogKeys = new HashSet<Long>();
        for (ChatMessage msg : res) {
            dialogKeys.add(msg.getPeerType() + msg.getPeerId() * 10L);
        }
        DialogDescription[] descriptions = loadDialogs(dialogKeys.toArray(new Long[0]));
        HashSet<DialogDescription> all = new HashSet<DialogDescription>();
        Collections.addAll(all, descriptions);
        HashSet<DialogDescription> allChanged = new HashSet<DialogDescription>();
        HashSet<DialogDescription> changed = new HashSet<DialogDescription>();
        HashSet<DialogDescription> created = new HashSet<DialogDescription>();

        for (ChatMessage msg : res) {
            if (msg.getPeerType() != PeerType.PEER_USER && msg.getPeerType() != PeerType.PEER_CHAT) {
                continue;
            }
            DialogDescription description = findDescription(msg.getPeerType(), msg.getPeerId(), all);
            if (description == null) {
                // Hack to avoid recreating dialog when leaving group
                if (msg.getPeerType() == PeerType.PEER_CHAT) {
                    if (msg.getContentType() == ContentType.MESSAGE_SYSTEM && msg.getExtras() instanceof TLLocalActionChatDeleteUser) {
                        TLLocalActionChatDeleteUser user = (TLLocalActionChatDeleteUser) msg.getExtras();
                        if (user.getUserId() == application.getCurrentUid() && msg.getSenderId() == application.getCurrentUid()) {
                            continue;
                        }
                    }
                }

                description = new DialogDescription(msg.getPeerType(), msg.getPeerId());
                applyDescriptor(description, msg);
                if (dialogs != null) {
                    TLDialog dialog = EngineUtils.findDialog(dialogs, description.getPeerType(), description.getPeerId());
                    if (dialog != null) {
                        description.setUnreadCount(dialog.getUnreadCount());
                    }
                }
                created.add(description);
                allChanged.add(description);
                all.add(description);
            } else {
                if (description.getDate() <= msg.getDate()) {
                    applyDescriptor(description, msg);
                    updateDialog(description);
                    changed.add(description);
                    allChanged.add(description);
                }
            }
        }

//        for (DialogDescription description : changed) {
//            updateDialog(description);
//        }
//
//        for (DialogDescription description : created) {
//            updateOrCreateDialog(description);
//        }
        updateOrCreateDialog(allChanged.toArray(new DialogDescription[0]));
    }

    private static DialogDescription findDescription(int peerType, int peerId, HashSet<DialogDescription> descriptions) {
        for (DialogDescription description : descriptions) {
            if (description.getPeerType() == peerType && description.getPeerId() == peerId) {
                return description;
            }
        }

        return null;
    }
}