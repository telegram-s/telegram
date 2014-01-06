package org.telegram.android.core.engines;

import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.service.TLLocalActionEncryptedMessageDestructed;
import org.telegram.android.log.Logger;

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

    public DialogDescription[] getItems(int offset, int limit) {
        return database.getItems(offset, limit);
    }

    public DialogDescription[] getAll() {
        return database.getAll();
    }

    public DialogDescription[] getUnreadedRemotelyDescriptions() {
        return database.getUnreadedRemotelyDescriptions();
    }

    public DialogDescription loadDialog(int peerType, int peerId) {
        return database.loadDialog(peerType, peerId);
    }

    public DialogDescription[] loadDialogs(Long[] uniqIds) {
        return database.loadDialogs(uniqIds);
    }

    public void updateOrCreateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public void updateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public void deleteDialog(int peerType, int peerId) {
        database.deleteDialog(peerType, peerId);
        application.getDialogSource().getViewSource().removeItemByKey(peerType * 10L + peerId);
    }

    public void markDialogAsNonFailed(int peerType, int peerId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setFailure(false);
            updateDialog(description);
        }
    }

    public void applyDescriptor(DialogDescription description, ChatMessage msg) {
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

    public void updateDescriptorPending(ChatMessage msg) {
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

    public void updateDescriptorShort(ChatMessage msg) {
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

    public void updateDescriptorShortEnc(ChatMessage msg) {
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

    public void updateDescriptorDeleteUnsent(int peerType, int peerId, int localId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -localId) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    public void updateDescriptorDeleteSent(int peerType, int peerId, int mid) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == mid) {
                updateDescriptorFromScratch(peerType, peerId);
            }
        }
    }

    public void updateDescriptorSelfDestructed(int peerType, int peerId, int mid) {
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

    public void onMaxLocalViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setLastLocalViewedMessage(maxId);
            description.setUnreadCount(0);
            updateDialog(description);
        }
    }

    public void clearFirstUnreadMessage(int peerType, int peerId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setFirstUnreadMessage(0);
            description.setUnreadCount(0);
            updateDialog(description);
        }
    }

    public void onMaxRemoteViewed(int peerType, int peerId, int maxId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            description.setLastRemoteViewedMessage(maxId);
            updateDialog(description);
        }
    }

    public void updateDescriptorEncSent(int peerType, int peerId, int date, int databaseId) {
        DialogDescription description = loadDialog(peerType, peerId);
        if (description != null) {
            if (description.getTopMessageId() == -databaseId) {
                description.setMessageState(MessageState.SENT);
                description.setDate(date);
                updateDialog(description);
            }
        }
    }

    public void updateDescriptorSent(int peerType, int peerId, int date, int mid, int databaseId) {
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

    private void updateDescriptorFromScratch(int peerType, int peerId) {
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
}