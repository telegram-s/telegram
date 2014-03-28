package org.telegram.android.core.background;

import android.os.*;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.update.TLLocalMessageEncryptedSent;
import org.telegram.android.core.model.update.TLLocalMessageSent;
import org.telegram.android.core.model.update.TLLocalMessageSentStated;
import org.telegram.android.core.model.update.TLLocalMessagesSentStated;
import org.telegram.android.log.Logger;
import org.telegram.android.core.model.*;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.LastEmojiProcessor;
import org.telegram.api.*;
import org.telegram.api.engine.RpcCallbackEx;
import org.telegram.api.messages.*;
import org.telegram.api.requests.*;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.tl.TLBytes;
import org.telegram.tl.TLIntVector;
import org.telegram.tl.TLMethod;
import org.telegram.tl.TLObject;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 2:02
 */
public class MessageSender {

    private static final String TAG = "MessageSender";

    private static final int TIMEOUT = 5 * 60 * 1000;

    private Executor senderExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageSender#" + res.hashCode());
            return res;
        }
    });

    private TelegramApplication application;


    public MessageSender(TelegramApplication application) {
        this.application = application;
    }


    public void sendMessage(final ChatMessage message) {
        if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());

            message.setMessageTimeout(chat.getSelfDestructTime());
            application.getEngine().getMessagesEngine().update(message);
            application.getDataSourceKernel().onSourceUpdateMessage(message);

            TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
            decryptedMessage.setRandomId(message.getRandomId());
            decryptedMessage.setRandomBytes(new TLBytes(Entropy.generateSeed(32)));

            if (message.getContentType() == ContentType.MESSAGE_TEXT) {
                decryptedMessage.setMessage(message.getMessage());
                decryptedMessage.setMedia(new TLDecryptedMessageMediaEmpty());
            } else if (message.getContentType() == ContentType.MESSAGE_GEO) {
                TLLocalGeo point = (TLLocalGeo) message.getExtras();
                decryptedMessage.setMessage("");
                decryptedMessage.setMedia(new TLDecryptedMessageMediaGeoPoint(point.getLatitude(), point.getLongitude()));
            }

            byte[] bundle;
            try {
                bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }


            TLRequestMessagesSendEncrypted request = new TLRequestMessagesSendEncrypted(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(),
                    new TLBytes(bundle));

            final long start = SystemClock.uptimeMillis();
            application.getApi().doRpcCall(request, TIMEOUT, new RpcCallbackEx<TLAbsSentEncryptedMessage>() {
                boolean isNotified = false;

                @Override
                public void onResult(TLAbsSentEncryptedMessage result) {
                    application.getUpdateProcessor().onMessage(new TLLocalMessageEncryptedSent(result, message));
                    Logger.d(TAG, "Chat message sent in time: " + (SystemClock.uptimeMillis() - start) + " ms");
                    if (!isNotified) {
                        isNotified = true;
                        application.getNotifications().notifySent(message.getPeerType(), message.getPeerId());
                    }
                }

                @Override
                public void onError(int errorCode, String errorMessage) {
                    application.getEngine().onMessageFailure(message);
                    application.notifyUIUpdate();
                    Logger.d(TAG, "Chat message sent failured in time: " + (SystemClock.uptimeMillis() - start) + " ms");
                }

                @Override
                public void onConfirmed() {
                    application.getEngine().onConfirmed(message);
                    application.notifyUIUpdate();
                    Logger.d(TAG, "Chat message confirm time: " + (SystemClock.uptimeMillis() - start) + " ms");
                    if (!isNotified) {
                        isNotified = true;
                        application.getNotifications().notifySent(message.getPeerType(), message.getPeerId());
                    }
                }
            });
        } else {
            TLAbsInputPeer peer = null;
            if (message.getPeerType() == PeerType.PEER_USER) {
                User user = application.getEngine().getUser(message.getPeerId());
                peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
            } else {
                peer = new TLInputPeerChat(message.getPeerId());
            }

            TLMethod request;
            if (message.getRawContentType() == ContentType.MESSAGE_CONTACT) {
                TLLocalContact contact = (TLLocalContact) message.getExtras();
                request = new TLRequestMessagesSendMedia(peer, new TLInputMediaContact(contact.getPhoneNumber(), contact.getFirstName(), contact.getLastName()), message.getRandomId());
            } else if (message.isForwarded()) {
                TLIntVector mids = new TLIntVector();
                mids.add(message.getForwardMid());
                request = new TLRequestMessagesForwardMessages(peer, mids);
            } else if (message.getRawContentType() == ContentType.MESSAGE_GEO) {
                TLLocalGeo point = (TLLocalGeo) message.getExtras();
                request = new TLRequestMessagesSendMedia(peer, new TLInputMediaGeoPoint(new TLInputGeoPoint(point.getLatitude(), point.getLongitude())), message.getRandomId());
            } else {
                request = new TLRequestMessagesSendMessage(peer, message.getMessage(), message.getRandomId());
            }
            final long start = SystemClock.uptimeMillis();

            application.getApi().doRpcCall(request, TIMEOUT, new RpcCallbackEx<TLObject>() {

                boolean isNotified = false;

                @Override
                public void onResult(TLObject object) {
                    if (object instanceof TLAbsSentMessage) {
                        TLAbsSentMessage sent = (TLAbsSentMessage) object;
                        application.getUpdateProcessor().onMessage(new TLLocalMessageSent(sent, message));
                    } else if (object instanceof TLAbsStatedMessage) {
                        TLAbsStatedMessage sent = (TLAbsStatedMessage) object;
                        application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));
                    } else {
                        TLAbsStatedMessages sent = (TLAbsStatedMessages) object;
                        application.getUpdateProcessor().onMessage(new TLLocalMessagesSentStated(sent, message));
                    }

                    if (!isNotified) {
                        isNotified = true;
                        application.getNotifications().notifySent(message.getPeerType(), message.getPeerId());
                    }
                }

                @Override
                public void onError(int errorCode, String errorMessage) {
                    application.getEngine().onMessageFailure(message);
                    application.notifyUIUpdate();

                    Logger.d(TAG, "Chat message sent failured in time: " + (SystemClock.uptimeMillis() - start) + " ms");
                }

                @Override
                public void onConfirmed() {
                    application.getEngine().onConfirmed(message);
                    application.notifyUIUpdate();
                    Logger.d(TAG, "Chat message confirm time: " + (SystemClock.uptimeMillis() - start) + " ms");

                    if (!isNotified) {
                        isNotified = true;
                        application.getNotifications().notifySent(message.getPeerType(), message.getPeerId());
                    }
                }
            });
        }
    }

    public void postTextMessage(int peerType, int peerId, final String message) {
        final ChatMessage msg = application.getEngine().prepareAsyncSendMessage(peerType, peerId, message);
        senderExecutor.execute(new Runnable() {
            @Override
            public void run() {
                application.getUiKernel().getLastEmoji().applyLastSmileys(
                        EmojiProcessor.findFirstUniqEmoji(message, LastEmojiProcessor.LAST_EMOJI_COUNT));
                application.getEngine().offThreadSendMessage(msg);
                sendMessage(msg);
                application.notifyUIUpdate();
            }
        });
    }

    public void reset() {

    }
}
