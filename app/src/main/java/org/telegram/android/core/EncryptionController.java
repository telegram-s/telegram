package org.telegram.android.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.*;
import org.telegram.android.core.model.media.TLLocalEncryptedFileLocation;
import org.telegram.android.core.model.media.TLLocalFileEmpty;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.core.model.service.TLLocalActionEncryptedTtl;
import org.telegram.android.log.Logger;
import org.telegram.api.*;
import org.telegram.api.messages.TLDhConfig;
import org.telegram.api.requests.TLRequestMessagesAcceptEncryption;
import org.telegram.api.requests.TLRequestMessagesGetDhConfig;
import org.telegram.api.requests.TLRequestMessagesRequestEncryption;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.tl.StreamingUtils;
import org.telegram.tl.TLObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import static org.telegram.mtproto.secure.CryptoUtils.*;
import static org.telegram.tl.StreamingUtils.readLong;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.10.13
 * Time: 19:06
 */
public class EncryptionController {
    private static final String TAG = "Encryption";

    private StelsApplication application;

    public EncryptionController(StelsApplication application) {
        this.application = application;
    }

    public void onUpdateEncryption(TLAbsEncryptedChat chat) {
        int id = chat.getId();
        EncryptedChat encChat = application.getEngine().getEncryptedChat(id);
        if (encChat != null && encChat.getState() == EncryptedChatState.WAITING && chat instanceof TLEncryptedChat) {
            enableEncryption(encChat, (TLEncryptedChat) chat);
        }
        application.getEngine().updateEncryptedChat(chat);
        application.getEncryptedChatProcessor().checkChats();
    }

    public byte[] encryptMessage(TLAbsDecryptedMessage message, int chatId) throws IOException {
        EncryptedChat chat = application.getEngine().getEncryptedChat(chatId);
        if (chat == null) {
            return null;
        }

        byte[] rawMessage = message.serialize();
        byte[] msgWithLen = CryptoUtils.concat(StreamingUtils.intToBytes(rawMessage.length), rawMessage);
        byte[] allMessage = CryptoUtils.align(msgWithLen, 16);
        byte[] msgKey = substring(CryptoUtils.SHA1(msgWithLen), 4, 16);

        byte[] key = chat.getKey();
        byte[] fingerprint = substring(SHA1(key), 12, 8);

        byte[] sha1_a = SHA1(msgKey, substring(key, 0, 32));
        byte[] sha1_b = SHA1(substring(key, 32, 16), msgKey, substring(key, 48, 16));
        byte[] sha1_c = SHA1(substring(key, 64, 32), msgKey);
        byte[] sha1_d = SHA1(msgKey, substring(key, 96, 32));
        byte[] aesKey = concat(substring(sha1_a, 0, 8), substring(sha1_b, 8, 12), substring(sha1_c, 4, 12));
        byte[] aesIv = concat(substring(sha1_a, 8, 12), substring(sha1_b, 0, 8), substring(sha1_c, 16, 4), substring(sha1_d, 0, 8));

        byte[] encrypted = CryptoUtils.AES256IGEEncrypt(allMessage, aesIv, aesKey);

        return concat(fingerprint, msgKey, encrypted);
    }

    public byte[] decryptMessage(byte[] rawData, int chatId) throws IOException {
        EncryptedChat chat = application.getEngine().getEncryptedChat(chatId);
        if (chat == null) {
            return null;
        }

        byte[] key = chat.getKey();
        long originalFingerprint = readLong(substring(SHA1(key), 12, 8), 0);

        long keyFingerprint = readLong(substring(rawData, 0, 8), 0);
        byte[] msgKey = substring(rawData, 8, 16);
        byte[] msg = substring(rawData, 24, rawData.length - 24);

        if (keyFingerprint != originalFingerprint) {
            Logger.w(TAG, "Ignoring message: mismatched fingerprints");
            return null;
        }

        byte[] sha1_a = SHA1(msgKey, substring(key, 0, 32));
        byte[] sha1_b = SHA1(substring(key, 32, 16), msgKey, substring(key, 48, 16));
        byte[] sha1_c = SHA1(substring(key, 64, 32), msgKey);
        byte[] sha1_d = SHA1(msgKey, substring(key, 96, 32));
        byte[] aesKey = concat(substring(sha1_a, 0, 8), substring(sha1_b, 8, 12), substring(sha1_c, 4, 12));
        byte[] aesIv = concat(substring(sha1_a, 8, 12), substring(sha1_b, 0, 8), substring(sha1_c, 16, 4), substring(sha1_d, 0, 8));

        byte[] decrypted = AES256IGEDecrypt(msg, aesIv, aesKey);
        int len = StreamingUtils.readInt(decrypted);
        byte[] rawMessage = substring(decrypted, 4, len);
        byte[] decrSha = substring(SHA1(substring(decrypted, 0, len + 4)), 4, 16);


        if (!arrayEq(decrSha, msgKey)) {
            Logger.w(TAG, "Ignoring message: mismatched message keys");
            return null;
        }

        return rawMessage;
    }

    public void onEncryptedMessage(TLAbsEncryptedMessage message) {
        if (message instanceof TLEncryptedMessage) {
            TLEncryptedMessage encMsg = (TLEncryptedMessage) message;
            EncryptedChat chat = application.getEngine().getEncryptedChat(encMsg.getChatId());
            if (chat == null) {
                return;
            }

            try {
                byte[] rawMessage = decryptMessage(encMsg.getBytes(), encMsg.getChatId());

                if (rawMessage == null) {
                    return;
                }

                TLObject object = application.getApi().getApiContext().deserializeMessage(rawMessage);
                if (object instanceof TLDecryptedMessageLayer) {
                    object = ((TLDecryptedMessageLayer) object).getMessage();
                }

                if (object instanceof TLDecryptedMessage) {
                    TLDecryptedMessage decryptedMessage = (TLDecryptedMessage) object;
                    if (decryptedMessage.getMedia() instanceof TLDecryptedMessageMediaGeoPoint) {
                        TLDecryptedMessageMediaGeoPoint geoPoint = (TLDecryptedMessageMediaGeoPoint) decryptedMessage.getMedia();
                        application.getEngine().onNewLocationEncMessage(PeerType.PEER_USER_ENCRYPTED, encMsg.getChatId(), decryptedMessage.getRandomId(), encMsg.getDate(), chat.getUserId(), chat.getSelfDestructTime(), geoPoint.getLon(), geoPoint.getLat());
                        User u = application.getEngine().getUser(chat.getUserId());
                        application.getNotifications().onNewSecretMessageGeo(u.getDisplayName(), u.getUid(), chat.getId(), u.getPhoto());
                    } else if (decryptedMessage.getMedia() instanceof TLDecryptedMessageMediaPhoto) {
                        TLDecryptedMessageMediaPhoto photo = (TLDecryptedMessageMediaPhoto) decryptedMessage.getMedia();
                        TLLocalPhoto localPhoto = new TLLocalPhoto();
                        if (photo.getThumbW() != 0 && photo.getThumbH() != 0) {
                            localPhoto.setFastPreviewW(photo.getThumbW());
                            localPhoto.setFastPreviewH(photo.getThumbH());
                            localPhoto.setFastPreviewKey(decryptedMessage.getRandomId() + "_photo");
                            localPhoto.setFastPreview(photo.getThumb());
                        } else {
                            localPhoto.setFastPreviewW(0);
                            localPhoto.setFastPreviewH(0);
                            localPhoto.setFastPreviewKey("");
                            localPhoto.setFastPreview(new byte[0]);
                        }

                        if (encMsg.getFile() instanceof TLEncryptedFile) {
                            TLEncryptedFile file = (TLEncryptedFile) encMsg.getFile();
                            localPhoto.setFullW(photo.getW());
                            localPhoto.setFullH(photo.getH());
                            byte[] digest = MD5Raw(concat(photo.getKey(), photo.getIv()));
                            int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)));
                            if (file.getKeyFingerprint() != fingerprint) {
                                Logger.w(TAG, "Ignoring message: attach fingerprint mismatched");
                                return;
                            }
                            localPhoto.setFullLocation(new TLLocalEncryptedFileLocation(file.getId(), file.getAccessHash(), photo.getSize(), file.getDcId(), photo.getKey(), photo.getIv()));
                        } else {
                            localPhoto.setFullH(0);
                            localPhoto.setFullW(0);
                            localPhoto.setFullLocation(new TLLocalFileEmpty());
                        }

                        application.getEngine().onNewPhotoEncMessage(PeerType.PEER_USER_ENCRYPTED, encMsg.getChatId(), decryptedMessage.getRandomId(), encMsg.getDate(), chat.getUserId(), chat.getSelfDestructTime(), localPhoto);
                        User u = application.getEngine().getUser(chat.getUserId());
                        application.getNotifications().onNewSecretMessagePhoto(u.getDisplayName(), u.getUid(), chat.getId(), u.getPhoto());
                    } else if (decryptedMessage.getMedia() instanceof TLDecryptedMessageMediaVideo) {
                        TLDecryptedMessageMediaVideo video = (TLDecryptedMessageMediaVideo) decryptedMessage.getMedia();
                        TLLocalVideo localVideo = new TLLocalVideo();

                        localVideo.setDuration(video.getDuration());
                        if (encMsg.getFile() instanceof TLEncryptedFile) {
                            TLEncryptedFile file = (TLEncryptedFile) encMsg.getFile();
                            byte[] digest = MD5Raw(concat(video.getKey(), video.getIv()));
                            int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)));
                            if (file.getKeyFingerprint() != fingerprint) {
                                Logger.w(TAG, "Ignoring message: attach fingerprint mismatched");
                                return;
                            }
                            localVideo.setVideoLocation(new TLLocalEncryptedFileLocation(file.getId(), file.getAccessHash(), file.getSize(), file.getDcId(), video.getKey(), video.getIv()));
                        } else {
                            localVideo.setVideoLocation(new TLLocalFileEmpty());
                        }

                        if (video.getThumbH() != 0 && video.getThumbW() != 0 && video.getThumb().length > 0) {
                            Bitmap src = BitmapFactory.decodeByteArray(video.getThumb(), 0, video.getThumb().length);
                            if (src != null && src.getHeight() > 0 && src.getWidth() > 0) {
                                if (src.getWidth() < 90 && src.getHeight() < 90) {
                                    Bitmap dest = Bitmap.createBitmap(90, 90, Bitmap.Config.ARGB_8888);
                                    Canvas canvas = new Canvas(dest);
                                    canvas.drawBitmap(dest, 0, 0, new Paint());
                                }
                                localVideo.setPreviewH(src.getHeight());
                                localVideo.setPreviewW(src.getWidth());
                                localVideo.setFastPreview(video.getThumb());
                            } else {
                                localVideo.setPreviewH(0);
                                localVideo.setPreviewW(0);
                                localVideo.setFastPreview(new byte[0]);
                            }
                        } else {
                            localVideo.setPreviewH(0);
                            localVideo.setPreviewW(0);
                            localVideo.setFastPreview(new byte[0]);
                        }
                        localVideo.setPreviewKey(decryptedMessage.getRandomId() + "_video");
                        localVideo.setPreviewLocation(new TLLocalFileEmpty());
                        application.getEngine().onNewVideoEncMessage(PeerType.PEER_USER_ENCRYPTED, encMsg.getChatId(), decryptedMessage.getRandomId(), encMsg.getDate(), chat.getUserId(), chat.getSelfDestructTime(), localVideo);
                        User u = application.getEngine().getUser(chat.getUserId());
                        application.getNotifications().onNewSecretMessageVideo(u.getDisplayName(), u.getUid(), chat.getId(), u.getPhoto());
                    } else if (decryptedMessage.getMedia() instanceof TLDecryptedMessageMediaEmpty) {
                        application.getEngine().onNewShortEncMessage(PeerType.PEER_USER_ENCRYPTED, encMsg.getChatId(), decryptedMessage.getRandomId(), encMsg.getDate(), chat.getUserId(), chat.getSelfDestructTime(), decryptedMessage.getMessage());
                        User u = application.getEngine().getUser(chat.getUserId());
                        application.getNotifications().onNewSecretMessage(u.getDisplayName(), u.getUid(), chat.getId(), u.getPhoto());
                    }

                    if (application.getUiKernel().getOpenedChatPeerType() == PeerType.PEER_USER_ENCRYPTED && application.getUiKernel().getOpenedChatPeerId() == chat.getId()) {
                        int date = application.getEngine().getMaxDateInDialog(PeerType.PEER_USER_ENCRYPTED, chat.getId());
                        application.getEngine().onMaxLocalViewed(PeerType.PEER_USER_ENCRYPTED, chat.getId(), Math.max(date, encMsg.getDate()));
                        application.getActions().readHistory(PeerType.PEER_USER_ENCRYPTED, chat.getId());
                    } else {
                        application.getEngine().onNewUnreadEncMessage(chat.getId(), encMsg.getDate());
                    }
                }
            } catch (IOException e) {
                Logger.t(TAG, e);
            }

            application.getActions().readHistory(PeerType.PEER_USER_ENCRYPTED, chat.getId());
            application.getTypingStates().resetEncryptedTyping(chat.getId());
        } else if (message instanceof TLEncryptedMessageService) {
            TLEncryptedMessageService service = (TLEncryptedMessageService) message;
            EncryptedChat chat = application.getEngine().getEncryptedChat(service.getChatId());
            if (chat == null) {
                return;
            }

            try {

                byte[] rawMessage = decryptMessage(service.getBytes(), service.getChatId());

                if (rawMessage == null) {
                    return;
                }

                TLObject object = application.getApi().getApiContext().deserializeMessage(rawMessage);
                if (object instanceof TLDecryptedMessageLayer) {
                    object = ((TLDecryptedMessageLayer) object).getMessage();
                }
                if (object instanceof TLDecryptedMessageService) {
                    TLDecryptedMessageService tlDecryptedMessageService = (TLDecryptedMessageService) object;
                    if (tlDecryptedMessageService.getAction() instanceof TLDecryptedMessageActionSetMessageTTL) {
                        TLDecryptedMessageActionSetMessageTTL ttlAction = (TLDecryptedMessageActionSetMessageTTL) tlDecryptedMessageService.getAction();
                        application.getEngine().setSelfDestructTimer(chat.getId(), ttlAction.getTtlSeconds());
                        application.getEngine().onNewInternalServiceMessage(PeerType.PEER_USER_ENCRYPTED, chat.getId(),
                                chat.getUserId(),
                                service.getDate(),
                                new TLLocalActionEncryptedTtl(ttlAction.getTtlSeconds()));
                    }

                    if (application.getUiKernel().getOpenedChatPeerType() == PeerType.PEER_USER_ENCRYPTED && application.getUiKernel().getOpenedChatPeerId() == chat.getId()) {
                        int date = application.getEngine().getMaxDateInDialog(PeerType.PEER_USER_ENCRYPTED, chat.getId());
                        application.getEngine().onMaxLocalViewed(PeerType.PEER_USER_ENCRYPTED, chat.getId(), Math.max(date, service.getDate()));
                        application.getActions().readHistory(PeerType.PEER_USER_ENCRYPTED, chat.getId());
                    } else {
                        application.getEngine().onNewUnreadEncMessage(chat.getId(), service.getDate());
                    }
                }
            } catch (Exception e) {
                Logger.t(TAG, e);
            }
        }
    }

    private void enableEncryption(EncryptedChat encChat, TLEncryptedChat encryptedChat) {
        ByteArrayInputStream stream = new ByteArrayInputStream(encChat.getKey());
        byte[] prime;
        byte[] rawA;
        try {
            int primeLen = StreamingUtils.readInt(stream);
            prime = StreamingUtils.readBytes(primeLen, stream);
            int aLen = StreamingUtils.readInt(stream);
            rawA = StreamingUtils.readBytes(aLen, stream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BigInteger a = loadBigInt(rawA);
        BigInteger gb = loadBigInt(encryptedChat.getGAOrB());

        BigInteger dhPrime = loadBigInt(prime);

        byte[] key = xor(alignKeyZero(fromBigInt(gb.modPow(a, dhPrime)), 256), encryptedChat.getNonce());
        long keyF = readLong(substring(CryptoUtils.SHA1(key), 12, 8), 0);

        application.getEngine().updateEncryptedChatKey(encryptedChat, key);
        User user = application.getEngine().getUser(encChat.getUserId());
        Logger.d(TAG, "Complete encryption: " + keyF);
    }

    public void confirmEncryption(int chatId) throws IOException {
        EncryptedChat chat = application.getEngine().getEncryptedChat(chatId);

        byte[] rawGa;
        byte[] nonce;
        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(chat.getKey());
            int primeLen = StreamingUtils.readInt(stream);
            rawGa = StreamingUtils.readBytes(primeLen, stream);
            int aLen = StreamingUtils.readInt(stream);
            nonce = StreamingUtils.readBytes(aLen, stream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        TLDhConfig dhConfig = (TLDhConfig) application.getApi().doRpcCall(new TLRequestMessagesGetDhConfig(0, 256));

        BigInteger g = new BigInteger("" + dhConfig.getG());
        BigInteger dhPrime = loadBigInt(dhConfig.getP());

        BigInteger ga = CryptoUtils.loadBigInt(rawGa);
        BigInteger b = CryptoUtils.loadBigInt(Entropy.generateSeed(dhConfig.getRandom()));
        BigInteger gb = g.modPow(b, dhPrime);

        byte[] key = xor(alignKeyZero(CryptoUtils.fromBigInt(ga.modPow(b, dhPrime)), 256), nonce);
        long keyF = readLong(substring(CryptoUtils.SHA1(key), 12, 8), 0);

        Logger.d(TAG, "Confirming encryption: " + keyF);

        TLAbsEncryptedChat encryptedChat = application.getApi().doRpcCall(new TLRequestMessagesAcceptEncryption(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), gb.toByteArray(), keyF));

        application.getEngine().updateEncryptedChat(encryptedChat);
        application.getEngine().updateEncryptedChatKey(encryptedChat, key);
        application.notifyUIUpdate();
    }

    public int requestEncryption(int uid) throws IOException {
        User user = application.getEngine().getUser(uid);
        TLDhConfig dhConfig = (TLDhConfig) application.getApi().doRpcCall(new TLRequestMessagesGetDhConfig(0, 256));

        BigInteger g = new BigInteger("" + dhConfig.getG());
        byte[] prime = dhConfig.getP();
        BigInteger dhPrime = loadBigInt(prime);

        byte[] rawA = Entropy.generateSeed(256);
        BigInteger a = loadBigInt(rawA);
        BigInteger ga = g.modPow(a, dhPrime);

        TLAbsEncryptedChat chat = application.getApi().doRpcCall(
                new TLRequestMessagesRequestEncryption(
                        new TLInputUserForeign(user.getUid(), user.getAccessHash()), Entropy.randomInt(), ga.toByteArray()));
        onUpdateEncryption(chat);

        byte[] tmpData = concat(StreamingUtils.intToBytes(prime.length), prime, StreamingUtils.intToBytes(rawA.length), rawA);
        application.getEngine().updateEncryptedChatKey(chat, tmpData);
        application.notifyUIUpdate();
        return chat.getId();
    }
}
