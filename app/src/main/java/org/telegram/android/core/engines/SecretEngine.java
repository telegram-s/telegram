package org.telegram.android.core.engines;

import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.service.TLLocalActionEncryptedCancelled;
import org.telegram.android.core.model.service.TLLocalActionEncryptedCreated;
import org.telegram.android.core.model.service.TLLocalActionEncryptedRequested;
import org.telegram.android.core.model.service.TLLocalActionEncryptedWaiting;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.tl.StreamingUtils;

import java.sql.SQLException;

/**
 * Created by ex3ndr on 03.01.14.
 */
public class SecretEngine {
    private static final String TAG = "SecretEngine";

    private SecretDatabase secretDatabase;
    private ModelEngine engine;
    private StelsApplication application;

    public SecretEngine(ModelEngine engine) {
        this.engine = engine;
        this.application = engine.getApplication();
        this.secretDatabase = new SecretDatabase(engine);
    }

    public EncryptedChat[] getPendingEncryptedChats() {
        return secretDatabase.getPendingEncryptedChats();
    }

    public void deleteEncryptedChat(int chatId) {
        secretDatabase.deleteChat(chatId);
    }

    public EncryptedChat getEncryptedChat(int chatId) {
        return secretDatabase.loadChat(chatId);
    }

    public EncryptedChat[] getEncryptedChats(int[] chatIds) {
        return secretDatabase.loadChats(chatIds);
    }

    public EncryptedChat[] getEncryptedChats(Integer[] chatIds) {
        return secretDatabase.loadChats(chatIds);
    }

    public void setSelfDestructTimer(int chatId, int time) {
        EncryptedChat chat = getEncryptedChat(chatId);
        chat.setSelfDestructTime(time);
        secretDatabase.updateChat(chat);
    }

    public void updateEncryptedChatKey(TLAbsEncryptedChat chat, byte[] key) {
        int id = chat.getId();
        EncryptedChat encryptedChat = secretDatabase.loadChat(chat.getId());
        if (encryptedChat != null) {
            encryptedChat.setKey(key);
            secretDatabase.updateChat(encryptedChat);
        }
    }

    public EncryptedChat loadChat(int id) {
        return secretDatabase.loadChat(id);
    }

    public void updateEncryptedChat(TLAbsEncryptedChat chat) {
        int id = chat.getId();

        if (id == 0) {
            Logger.w(TAG, "Ignoring encrypted chat");
            return;
        }

        int date = (int) (TimeOverlord.getInstance().getServerTime() / 1000);

        if (chat instanceof TLEncryptedChat) {
            date = ((TLEncryptedChat) chat).getDate();
        } else if (chat instanceof TLEncryptedChatRequested) {
            date = ((TLEncryptedChatRequested) chat).getDate();
        } else if (chat instanceof TLEncryptedChatWaiting) {
            date = ((TLEncryptedChatWaiting) chat).getDate();
        }

        EncryptedChat encryptedChat = secretDatabase.loadChat(chat.getId());
        if (encryptedChat != null) {
            if (!(chat instanceof TLEncryptedChat) && !(chat instanceof TLEncryptedChatDiscarded)) {
                return;
            }
            writeEncryptedChatInfo(encryptedChat, chat);
            secretDatabase.updateChat(encryptedChat);
        } else {
            if (!(chat instanceof TLEncryptedChatWaiting) && !(chat instanceof TLEncryptedChatRequested)) {
                return;
            }
            encryptedChat = new EncryptedChat();
            encryptedChat.setId(id);
            writeEncryptedChatInfo(encryptedChat, chat);
            secretDatabase.createChat(encryptedChat);
        }
        application.getEncryptedChatSource().notifyChatChanged(id);

        DialogDescription description = engine.getDescriptionForPeer(PeerType.PEER_USER_ENCRYPTED, encryptedChat.getId());
        if (description == null) {
            description = createDescriptionForEncryptedUser(encryptedChat.getId(), encryptedChat.getUserId());
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            engine.getDialogsEngine().updateOrCreateDialog(description);

            if (encryptedChat.getState() == EncryptedChatState.REQUESTED) {
                User user = engine.getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatRequested(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            }
        } else {
            description.setDate(date);
            switch (encryptedChat.getState()) {
                default:
                case EncryptedChatState.EMPTY:
                    description.setMessage("Encrypted chat");
                    break;
                case EncryptedChatState.REQUESTED:
                    description.setMessage("Requested new chat");
                    description.setExtras(new TLLocalActionEncryptedRequested());
                    break;
                case EncryptedChatState.WAITING:
                    description.setMessage("Waiting to approve");
                    description.setExtras(new TLLocalActionEncryptedWaiting());
                    break;
                case EncryptedChatState.DISCARDED:
                    description.setMessage("Chat discarded");
                    description.setExtras(new TLLocalActionEncryptedCancelled());
                    break;
                case EncryptedChatState.NORMAL:
                    description.setMessage("Chat established");
                    description.setExtras(new TLLocalActionEncryptedCreated());
                    break;
            }
            description.setContentType(ContentType.MESSAGE_SYSTEM);
            engine.getDialogsEngine().updateOrCreateDialog(description);

            if (encryptedChat.getState() == EncryptedChatState.NORMAL) {
                User user = engine.getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatEstablished(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            } else if (encryptedChat.getState() == EncryptedChatState.DISCARDED) {
                User user = engine.getUser(encryptedChat.getUserId());
                application.getNotifications().onNewSecretChatCancelled(user.getDisplayName(), user.getUid(), encryptedChat.getId(), user.getPhoto());
            }
        }
    }

    private void writeEncryptedChatInfo(EncryptedChat chat, TLAbsEncryptedChat rawChat) {
        if (rawChat instanceof TLEncryptedChatRequested) {
            TLEncryptedChatRequested requested = (TLEncryptedChatRequested) rawChat;
            chat.setAccessHash(requested.getAccessHash());
            chat.setUserId(requested.getAdminId());
            byte[] tmpKey = CryptoUtils.concat(
                    StreamingUtils.intToBytes(requested.getGA().length),
                    requested.getGA(),
                    StreamingUtils.intToBytes(requested.getNonce().length),
                    requested.getNonce());
            chat.setKey(tmpKey);
            chat.setState(EncryptedChatState.REQUESTED);
            chat.setOut(false);
        } else if (rawChat instanceof TLEncryptedChatWaiting) {
            TLEncryptedChatWaiting waiting = (TLEncryptedChatWaiting) rawChat;
            chat.setAccessHash(waiting.getAccessHash());
            chat.setUserId(waiting.getParticipantId());
            chat.setState(EncryptedChatState.WAITING);
            chat.setOut(true);
        } else if (rawChat instanceof TLEncryptedChatDiscarded) {
            chat.setState(EncryptedChatState.DISCARDED);
        } else if (rawChat instanceof TLEncryptedChat) {
            chat.setState(EncryptedChatState.NORMAL);
        }
    }


    private DialogDescription createDescriptionForEncryptedUser(int chatId, int uid) {
        DialogDescription res = new DialogDescription();
        res.setPeerType(PeerType.PEER_USER_ENCRYPTED);
        res.setPeerId(chatId);
        return res;
    }
}
