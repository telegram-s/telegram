package org.telegram.android.core.background;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.os.*;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.EncryptedChat;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.update.TLLocalMessageSentPhoto;
import org.telegram.android.core.model.update.TLLocalMessageSentStated;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.media.Optimizer;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.api.*;
import org.telegram.api.engine.file.UploadListener;
import org.telegram.api.engine.file.Uploader;
import org.telegram.api.messages.TLAbsSentEncryptedMessage;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.messages.TLSentEncryptedFile;
import org.telegram.api.requests.TLRequestMessagesSendEncryptedFile;
import org.telegram.api.requests.TLRequestMessagesSendMedia;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.tl.StreamingUtils;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.telegram.mtproto.secure.CryptoUtils.*;
import static org.telegram.mtproto.secure.CryptoUtils.substring;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class MediaSender {
    private static final String TAG = "MediaSender";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final HashMap<Integer, SendState> states = new HashMap<Integer, SendState>();

    private static final int TIMEOUT = 5 * 60 * 1000;

    private final Executor mediaExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageMediaSender#" + res.hashCode());
            return res;
        }
    });

    private StelsApplication application;

    private CopyOnWriteArrayList<WeakReference<SenderListener>> listeners = new CopyOnWriteArrayList<WeakReference<SenderListener>>();

    public MediaSender(StelsApplication application) {
        this.application = application;
    }

    public void sendMedia(final ChatMessage message) {
        SendState sendState = new SendState();
        sendState.isSent = false;
        sendState.isUploaded = false;
        sendState.uploadProgress = 0;
        states.put(message.getDatabaseId(), sendState);
        updateState(message.getDatabaseId(), sendState);
        mediaExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                if (message.getExtras() instanceof TLUploadingPhoto || message.getExtras() instanceof TLUploadingVideo
                        || message.getExtras() instanceof TLUploadingDocument) {
                    try {
                        if (message.getExtras() instanceof TLUploadingPhoto) {
                            if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                uploadEncryptedPhoto(message);
                            } else {
                                uploadPhoto(message);
                            }
                        } else if (message.getExtras() instanceof TLUploadingVideo) {
                            if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                uploadEncVideo(message);
                            } else {
                                uploadVideo(message);
                            }
                        } else if (message.getExtras() instanceof TLUploadingDocument) {
                            if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                uploadEncDocument(message);
                            } else {
                                uploadDocument(message);
                            }
                        }

                        synchronized (states) {
                            SendState state = states.get(message.getDatabaseId());
                            state.isSent = true;
                            updateState(message.getDatabaseId(), state);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        synchronized (states) {
                            SendState state = states.get(message.getDatabaseId());
                            if (state.isCanceled) {
                                return;
                            }
                        }
                        application.getEngine().onMessageFailure(message);
                    }

                    application.notifyUIUpdate();
                }
            }
        });
    }

    private void uploadDocument(ChatMessage message) throws Exception {
        TLUploadingDocument document = (TLUploadingDocument) message.getExtras();
        Uploader.UploadResult result = uploadFile(document.getFilePath(), message.getDatabaseId());
        TLAbsStatedMessage sent = doSendDoc(result, document, message);
        saveUploadedDocument(sent, document.getFilePath());
        completeDocSending(sent, message);
    }

    private void uploadEncDocument(ChatMessage message) throws Exception {
        EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());
        TLUploadingDocument document = (TLUploadingDocument) message.getExtras();
        EncryptedFile encryptedFile = encryptFile(document.getFilePath());
        Uploader.UploadResult result = uploadFile(encryptedFile.getFileName(), message.getDatabaseId());
        EncryptedDocSent sent = doSendEncDoc(result, document, encryptedFile, message, chat);
        saveUploadedEncDocument(sent, document.getFilePath());
        completeEncDocSending(sent, message, chat);
    }

    private void uploadPhoto(ChatMessage message) throws Exception {
        String uploadFileName = preparePhoto(message);
        Uploader.UploadResult result = uploadFile(uploadFileName, message.getDatabaseId());
        TLAbsStatedMessage sent = doSendPhoto(result, message);
        saveUploadedPhoto(sent, uploadFileName);
        completePhotoSending(sent, uploadFileName, message);
    }

    private void uploadEncryptedPhoto(ChatMessage message) throws Exception {
        EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());
        String uploadFileName = preparePhoto(message);
        EncryptedFile encryptedFile = encryptFile(uploadFileName);
        Uploader.UploadResult result = uploadFile(encryptedFile.getFileName(), message.getDatabaseId());
        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(uploadFileName);
        EncryptedPhotoSent encryptedMessage = doSendPhotoEnc(uploadFileName, encryptedFile, result, previewResult, message, chat);
        saveUploadedEncPhoto(encryptedMessage, uploadFileName);
        completeEncPhotoSending(encryptedMessage, message, chat);
    }

    private void uploadVideo(ChatMessage message) throws Exception {
        TLUploadingVideo srcVideo = (TLUploadingVideo) message.getExtras();
        VideoMetadata metadata = getVideoMetadata(srcVideo.getFileName());
        String fullPreview = writeTempFile(metadata.img);
        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(metadata.getImg());
        Uploader.UploadResult thumbResult = uploadFileSilent(writeTempFile(previewResult.getData()), message.getDatabaseId());
        Uploader.UploadResult mainResult = uploadFile(srcVideo.getFileName(), message.getDatabaseId());
        TLAbsStatedMessage sent = doSendVideo(mainResult, thumbResult, metadata, message);
        saveUploadedVideo(srcVideo.getFileName(), fullPreview, sent);
        completeVideoSending(sent, message);
    }

    private void uploadEncVideo(ChatMessage message) throws Exception {
        EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());

        TLUploadingVideo srcVideo = (TLUploadingVideo) message.getExtras();
        VideoMetadata metadata = getVideoMetadata(srcVideo.getFileName());
        EncryptedFile encryptedFile = encryptFile(srcVideo.getFileName());
        String fullPreview = writeTempFile(metadata.img);
        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(metadata.getImg());
        Uploader.UploadResult mainResult = uploadFile(encryptedFile.getFileName(), message.getDatabaseId());

        EncryptedVideoSent videoSent = doSendEncVideo(mainResult, encryptedFile, srcVideo.getFileName(), metadata, previewResult, chat, message);
        saveUploadedEncVideo(srcVideo.getFileName(), fullPreview, videoSent.getVideo());

        completeEncVideoSending(videoSent, message, chat);
    }

    private String preparePhoto(ChatMessage message) throws Exception {
        if (!(message.getExtras() instanceof TLUploadingPhoto)) {
            throw new InvalidObjectException("Expected TLUploadingPhoto extras");
        }

        TLUploadingPhoto uploadingPhoto = (TLUploadingPhoto) message.getExtras();
        String destFile = getUploadTempFile();
        if (uploadingPhoto.getFileUri() != null && uploadingPhoto.getFileUri().length() > 0) {
            Optimizer.optimize(uploadingPhoto.getFileUri(), application, destFile);
        } else {
            Optimizer.optimize(uploadingPhoto.getFileName(), destFile);
        }
        return destFile;
    }

    private TLAbsStatedMessage doSendDoc(Uploader.UploadResult result, TLUploadingDocument document, ChatMessage message) throws Exception {
        TLAbsInputPeer peer;
        if (message.getPeerType() == PeerType.PEER_USER) {
            User user = application.getEngine().getUser(message.getPeerId());
            peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
        } else {
            peer = new TLInputPeerChat(message.getPeerId());
        }

        TLAbsInputFile inputFile;
        if (result.isUsedBigFile()) {
            inputFile = new TLInputFileBig(result.getFileId(), result.getPartsCount(), document.getFileName());
        } else {
            inputFile = new TLInputFile(result.getFileId(), result.getPartsCount(), document.getFileName(), result.getHash());
        }


        String mimeType = "application/octet-stream";

        String ext = null;
        if (document.getFileName().indexOf('.') > -1) {
            ext = document.getFileName().substring(document.getFileName().lastIndexOf('.') + 1);
        }

        if (ext != null && ext.length() > 0) {
            String nMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (nMimeType != null) {
                mimeType = nMimeType;
            }
        }

        return application.getApi().doRpcCall(
                new TLRequestMessagesSendMedia(peer, new TLInputMediaUploadedDocument(inputFile, document.getFileName(), mimeType), message.getRandomId()), TIMEOUT);
    }

    private EncryptedDocSent doSendEncDoc(Uploader.UploadResult result, TLUploadingDocument document, EncryptedFile encryptedFile, ChatMessage message, EncryptedChat chat) throws Exception {
        String mimeType = "application/octet-stream";

        String ext = null;
        if (document.getFileName().indexOf('.') > -1) {
            ext = document.getFileName().substring(document.getFileName().lastIndexOf('.') + 1);
        }

        if (ext != null && ext.length() > 0) {
            String nMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (nMimeType != null) {
                mimeType = nMimeType;
            }
        }

        TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
        decryptedMessage.setRandomId(message.getRandomId());
        decryptedMessage.setRandomBytes(Entropy.generateSeed(32));
        decryptedMessage.setMessage("");

        decryptedMessage.setMedia(new TLDecryptedMessageMediaDocument(new byte[0], 0, 0,
                document.getFileName(), mimeType, document.getFileSize(), encryptedFile.getKey(), encryptedFile.getIv()));

        byte[] digest = MD5Raw(concat(encryptedFile.getKey(), encryptedFile.getIv()));
        int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)), 0);

        byte[] bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());

        TLAbsInputEncryptedFile inputEncryptedFile;
        if (result.isUsedBigFile()) {
            inputEncryptedFile = new TLInputEncryptedFileBigUploaded(result.getFileId(), result.getPartsCount(), fingerprint);
        } else {
            inputEncryptedFile = new TLInputEncryptedFileUploaded(result.getFileId(), result.getPartsCount(), result.getHash(), fingerprint);
        }

        TLAbsSentEncryptedMessage encryptedMessage = application.getApi().doRpcCall(new TLRequestMessagesSendEncryptedFile(
                new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(), bundle,
                inputEncryptedFile), TIMEOUT);

        TLLocalDocument localDocument = new TLLocalDocument();
        if (encryptedMessage instanceof TLSentEncryptedFile) {
            TLSentEncryptedFile file = (TLSentEncryptedFile) encryptedMessage;
            if (file.getFile() instanceof TLEncryptedFile) {
                TLEncryptedFile file1 = (TLEncryptedFile) file.getFile();
                localDocument.setFileLocation(new TLLocalEncryptedFileLocation(file1.getId(), file1.getAccessHash(), file1.getSize(), file1.getDcId(), encryptedFile.getKey(), encryptedFile.getIv()));
            } else {
                localDocument.setFileLocation(new TLLocalFileEmpty());
            }
        } else {
            localDocument.setFileLocation(new TLLocalFileEmpty());
        }

        localDocument.setFileName(document.getFileName());
        localDocument.setMimeType(mimeType);

        return new EncryptedDocSent(encryptedMessage, localDocument);
    }

    private TLAbsStatedMessage doSendPhoto(Uploader.UploadResult result, ChatMessage message) throws Exception {
        TLAbsInputPeer peer;
        if (message.getPeerType() == PeerType.PEER_USER) {
            User user = application.getEngine().getUser(message.getPeerId());
            peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
        } else {
            peer = new TLInputPeerChat(message.getPeerId());
        }

        TLAbsInputFile inputFile;
        if (result.isUsedBigFile()) {
            inputFile = new TLInputFileBig(result.getFileId(), result.getPartsCount(), "file.jpg");
        } else {
            inputFile = new TLInputFile(result.getFileId(), result.getPartsCount(), "file.jpg", result.getHash());
        }

        return application.getApi().doRpcCall(
                new TLRequestMessagesSendMedia(peer, new TLInputMediaUploadedPhoto(inputFile), message.getRandomId()), TIMEOUT);
    }

    private EncryptedPhotoSent doSendPhotoEnc(String originalFile, EncryptedFile encryptedFile, Uploader.UploadResult result, Optimizer.FastPreviewResult previewResult,
                                              ChatMessage message, EncryptedChat chat) throws Exception {
        TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
        decryptedMessage.setRandomId(message.getRandomId());
        decryptedMessage.setRandomBytes(Entropy.generateSeed(32));
        decryptedMessage.setMessage("");

        Point size = Optimizer.getSize(originalFile);
        decryptedMessage.setMedia(new TLDecryptedMessageMediaPhoto(previewResult.getData(),
                previewResult.getW(), previewResult.getH(), size.x, size.y, (int) new File(originalFile).length(),
                encryptedFile.getKey(), encryptedFile.getIv()));

        byte[] digest = MD5Raw(concat(encryptedFile.getKey(), encryptedFile.getIv()));
        int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)), 0);

        byte[] bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());

        TLAbsInputEncryptedFile inputEncryptedFile;
        if (result.isUsedBigFile()) {
            inputEncryptedFile = new TLInputEncryptedFileBigUploaded(result.getFileId(), result.getPartsCount(), fingerprint);
        } else {
            inputEncryptedFile = new TLInputEncryptedFileUploaded(result.getFileId(), result.getPartsCount(), result.getHash(), fingerprint);
        }

        TLAbsSentEncryptedMessage encryptedMessage = application.getApi().doRpcCall(new TLRequestMessagesSendEncryptedFile(
                new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(), bundle,
                inputEncryptedFile), TIMEOUT);

        TLLocalPhoto photo = new TLLocalPhoto();
        photo.setFastPreviewW(previewResult.getW());
        photo.setFastPreviewH(previewResult.getH());
        photo.setFastPreview(previewResult.getData());
        photo.setFastPreviewKey(decryptedMessage.getRandomId() + "_photo");
        photo.setFullW(size.x);
        photo.setFullH(size.y);
        if (encryptedMessage instanceof TLSentEncryptedFile) {
            TLSentEncryptedFile file = (TLSentEncryptedFile) encryptedMessage;
            if (file.getFile() instanceof TLEncryptedFile) {
                TLEncryptedFile file1 = (TLEncryptedFile) file.getFile();
                photo.setFullLocation(new TLLocalEncryptedFileLocation(file1.getId(), file1.getAccessHash(), file1.getSize(), file1.getDcId(), encryptedFile.getKey(), encryptedFile.getIv()));
            } else {
                photo.setFullLocation(new TLLocalFileEmpty());
            }
        } else {
            photo.setFullLocation(new TLLocalFileEmpty());
        }

        return new EncryptedPhotoSent(encryptedMessage, photo);
    }

    private TLAbsStatedMessage doSendVideo(Uploader.UploadResult mainResult, Uploader.UploadResult thumbResult, VideoMetadata metadata, ChatMessage message) throws Exception {
        TLInputMediaUploadedThumbVideo video = new TLInputMediaUploadedThumbVideo();
        if (mainResult.isUsedBigFile()) {
            video.setFile(new TLInputFileBig(mainResult.getFileId(), mainResult.getPartsCount(), "file.mp4"));
        } else {
            video.setFile(new TLInputFile(mainResult.getFileId(), mainResult.getPartsCount(), "file.mp4", mainResult.getHash()));
        }
        video.setThumb(new TLInputFile(thumbResult.getFileId(), thumbResult.getPartsCount(), "file.jpg", thumbResult.getHash()));
        video.setDuration((int) (metadata.getDuration() / 1000));
        video.setW(metadata.getWidth());
        video.setH(metadata.getHeight());

        TLAbsInputPeer peer;
        if (message.getPeerType() == PeerType.PEER_USER) {
            User user = application.getEngine().getUser(message.getPeerId());
            peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
        } else {
            peer = new TLInputPeerChat(message.getPeerId());
        }

        return application.getApi().doRpcCall(new TLRequestMessagesSendMedia(peer, video, message.getRandomId()));
    }

    private EncryptedVideoSent doSendEncVideo(Uploader.UploadResult mainResult, EncryptedFile encryptedFile, String srcFile, VideoMetadata metadata, Optimizer.FastPreviewResult previewResult, EncryptedChat chat, ChatMessage message) throws Exception {
        TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
        decryptedMessage.setRandomId(message.getRandomId());
        decryptedMessage.setRandomBytes(Entropy.generateSeed(32));
        decryptedMessage.setMessage("");
        TLDecryptedMessageMediaVideo video = new TLDecryptedMessageMediaVideo();
        video.setDuration((int) (metadata.getDuration() / 1000));
        video.setH(metadata.getHeight());
        video.setW(metadata.getWidth());
        video.setSize((int) new File(srcFile).length());
        video.setThumbW(previewResult.getW());
        video.setThumbH(previewResult.getH());
        video.setThumb(previewResult.getData());
        video.setKey(encryptedFile.getKey());
        video.setIv(encryptedFile.getIv());
        decryptedMessage.setMedia(video);

        byte[] digest = MD5Raw(concat(encryptedFile.getKey(), encryptedFile.getIv()));
        int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)), 0);

        byte[] bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());

        TLAbsInputEncryptedFile inputFile;
        if (mainResult.isUsedBigFile()) {
            inputFile = new TLInputEncryptedFileBigUploaded(mainResult.getFileId(), mainResult.getPartsCount(), fingerprint);
        } else {
            inputFile = new TLInputEncryptedFileUploaded(mainResult.getFileId(), mainResult.getPartsCount(), mainResult.getHash(), fingerprint);
        }

        TLAbsSentEncryptedMessage encryptedMessage = application.getApi().doRpcCall(
                new TLRequestMessagesSendEncryptedFile(
                        new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(), bundle, inputFile));

        TLLocalVideo localVideo = new TLLocalVideo();
        localVideo.setDuration((int) (metadata.getDuration() / 1000));
        localVideo.setPreviewW(previewResult.getW());
        localVideo.setPreviewH(previewResult.getH());
        localVideo.setFastPreview(previewResult.getData());
        localVideo.setPreviewKey(decryptedMessage.getRandomId() + "_video");
        localVideo.setPreviewLocation(new TLLocalFileEmpty());

        if (encryptedMessage instanceof TLSentEncryptedFile) {
            TLSentEncryptedFile file = (TLSentEncryptedFile) encryptedMessage;
            if (file.getFile() instanceof TLEncryptedFile) {
                TLEncryptedFile file1 = (TLEncryptedFile) file.getFile();
                localVideo.setVideoLocation(new TLLocalEncryptedFileLocation(file1.getId(), file1.getAccessHash(), file1.getSize(), file1.getDcId(), encryptedFile.getKey(), encryptedFile.getIv()));
            } else {
                localVideo.setVideoLocation(new TLLocalFileEmpty());
            }
        } else {
            localVideo.setVideoLocation(new TLLocalFileEmpty());
        }

        return new EncryptedVideoSent(encryptedMessage, localVideo);
    }

    private void saveUploadedPhoto(TLAbsStatedMessage sent, String uploadFileName) throws Exception {
        TLMessage msgRes = (TLMessage) sent.getMessage();
        TLLocalPhoto mediaPhoto = EngineUtils.convertPhoto((TLMessageMediaPhoto) msgRes.getMedia());
        if (!(mediaPhoto.getFullLocation() instanceof TLLocalFileEmpty)) {
            String downloadKey = DownloadManager.getPhotoKey(mediaPhoto);
            application.getDownloadManager().saveDownloadImagePreview(downloadKey, uploadFileName);
            application.getDownloadManager().saveDownloadImage(downloadKey, uploadFileName);

            try {
                if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                    application.getDownloadManager().writeToGallery(uploadFileName, downloadKey + ".jpg");
                }
            } catch (Exception e) {
                CrashHandler.logHandledException(e);
                Logger.t(TAG, e);
            }
        }
    }

    private void saveUploadedDocument(TLAbsStatedMessage sent, String uploadFileName) throws Exception {
        TLMessage msgRes = (TLMessage) sent.getMessage();
        TLLocalDocument mediaDoc = (TLLocalDocument) EngineUtils.convertMedia(msgRes.getMedia());
        if (!(mediaDoc.getFileLocation() instanceof TLLocalFileEmpty)) {
            String downloadKey = DownloadManager.getDocumentKey(mediaDoc);
            application.getDownloadManager().saveDownloadDoc(downloadKey, uploadFileName);
        }
    }

    private void saveUploadedEncDocument(EncryptedDocSent sent, String uploadFileName) throws Exception {
        if (sent.getDocument().getFileLocation() instanceof TLLocalEncryptedFileLocation) {
            String downloadKey = DownloadManager.getDocumentKey(sent.getDocument());
            application.getDownloadManager().saveDownloadDoc(downloadKey, uploadFileName);
        }
    }

    private void saveUploadedEncPhoto(EncryptedPhotoSent encryptedMessage, String uploadFileName) throws Exception {
        if (encryptedMessage.getPhoto().getFullLocation() instanceof TLLocalEncryptedFileLocation) {
            String downloadKey = DownloadManager.getPhotoKey(encryptedMessage.getPhoto());
            application.getDownloadManager().saveDownloadImagePreview(downloadKey, uploadFileName);
            application.getDownloadManager().saveDownloadImage(downloadKey, uploadFileName);
        }
    }

    private void saveUploadedVideo(String srcFile, String previewFile, TLAbsStatedMessage sent) throws Exception {
        TLMessage msgRes = (TLMessage) sent.getMessage();
        TLLocalVideo mediaVideo = EngineUtils.convertVideo((TLMessageMediaVideo) msgRes.getMedia());
        String downloadKey = DownloadManager.getVideoKey(mediaVideo);
        application.getDownloadManager().saveDownloadVideo(downloadKey, srcFile);
        application.getDownloadManager().saveDownloadImagePreview(downloadKey, previewFile);
        try {
            if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                application.getDownloadManager().writeToGallery(srcFile, downloadKey + ".mp4");
            }
        } catch (Exception e) {
            CrashHandler.logHandledException(e);
            Logger.t(TAG, e);
        }
    }

    private void saveUploadedEncVideo(String srcFile, String previewFile, TLLocalVideo localVideo) throws Exception {
        if (localVideo.getVideoLocation() instanceof TLLocalEncryptedFileLocation) {
            String downloadKey = DownloadManager.getVideoKey(localVideo);
            application.getDownloadManager().saveDownloadVideo(downloadKey, srcFile);
            application.getDownloadManager().saveDownloadImagePreview(downloadKey, previewFile);
            try {
                if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                    application.getDownloadManager().writeToGallery(srcFile, downloadKey + ".mp4");
                }
            } catch (IOException e) {
                CrashHandler.logHandledException(e);
                Logger.t(TAG, e);
            }
        }
    }

    private void completePhotoSending(TLAbsStatedMessage sent, String uploadFileName, ChatMessage message) {
        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(uploadFileName);
        application.getUpdateProcessor().onMessage(new TLLocalMessageSentPhoto(sent, message,
                previewResult.getData(), previewResult.getW(), previewResult.getH()));
    }

    private void completeDocSending(TLAbsStatedMessage sent, ChatMessage message) {
        application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));
    }

    private void completeEncDocSending(EncryptedDocSent encryptedMessage, ChatMessage message, EncryptedChat chat) {
        message.setMessageTimeout(chat.getSelfDestructTime());
        application.getEngine().onMessageEncDocSent(message, encryptedMessage.getEncryptedMessage().getDate(), encryptedMessage.getDocument());
    }

    private void completeEncPhotoSending(EncryptedPhotoSent encryptedMessage, ChatMessage message, EncryptedChat chat) {
        message.setMessageTimeout(chat.getSelfDestructTime());
        application.getEngine().onMessageEncPhotoSent(message, encryptedMessage.getEncryptedMessage().getDate(), encryptedMessage.getPhoto());
    }

    private void completeVideoSending(TLAbsStatedMessage sent, ChatMessage message) {
        application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));
    }

    private void completeEncVideoSending(EncryptedVideoSent sent, ChatMessage message, EncryptedChat chat) {
        message.setMessageTimeout(chat.getSelfDestructTime());
        application.getEngine().onMessageVideoSent(message, sent.getEncryptedMessage().getDate(), sent.getVideo());
    }

    private synchronized void updateState(final int localId, final SendState state) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (WeakReference<SenderListener> ref : listeners) {
                    SenderListener listener = ref.get();
                    if (listener != null) {
                        listener.onUploadStateChanged(localId, state);
                    }
                }
            }
        });
    }

    public void registerListener(SenderListener listener) {
        for (WeakReference<SenderListener> ref : listeners) {
            if (ref.get() == listener) {
                return;
            }
        }
        listeners.add(new WeakReference<SenderListener>(listener));
    }

    public void unregisterListener(SenderListener listener) {
        for (WeakReference<SenderListener> ref : listeners) {
            if (ref.get() == listener) {
                listeners.remove(ref);
                return;
            }
        }
    }

    public SendState getSendState(int databaseId) {
        return states.get(databaseId);
    }

    public boolean cancelUpload(int databaseId) {
        synchronized (states) {
            SendState state = states.get(databaseId);
            if (state != null) {
                if (!state.isUploaded) {
                    state.isCanceled = true;
                    application.getApi().getUploader().cancelTask(state.currentUploadId);
                    application.getEngine().cancelMediaSend(databaseId);
                    application.getKernel().getDataSourceKernel().notifyUIUpdate();
                    return true;
                }
            }
        }
        return false;
    }


    private String getUploadTempFile() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                //return getExternalCacheDir().getAbsolutePath();
                return ((File) StelsApplication.class.getMethod("getExternalCacheDir").invoke(application)).getAbsolutePath() + "/upload_" + Entropy.generateRandomId() + ".jpg";
            } catch (Exception e) {
                // Log.e(TAG, e);
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/cache/" + application.getPackageName() + "/upload_" + Entropy.generateRandomId() + ".jpg";
    }

    private String writeTempFile(Bitmap data) throws Exception {
        String fileName = getUploadTempFile();
        FileOutputStream outputStream = new FileOutputStream(fileName);
        data.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        outputStream.close();
        return fileName;
    }

    private String writeTempFile(byte[] data) throws Exception {
        String fileName = getUploadTempFile();
        RandomAccessFile thumbFile = new RandomAccessFile(fileName, "rw");
        thumbFile.write(data);
        thumbFile.close();
        return fileName;
    }

    private Uploader.UploadResult uploadFile(String fileName, final int localId) throws Exception {
        int taskId = application.getApi().getUploader().requestTask(fileName, new UploadListener() {
            @Override
            public void onPartUploaded(int percent, int downloadedSize) {
                SendState state = states.get(localId);
                state.uploadProgress = percent;
                updateState(localId, state);
            }
        });
        SendState state = states.get(localId);
        state.currentUploadId = taskId;
        application.getApi().getUploader().waitForTask(taskId);
        Uploader.UploadResult result = application.getApi().getUploader().getUploadResult(taskId);
        if (result == null) {
            throw new IOException("Unable to upload file");
        }

        return result;
    }

    private Uploader.UploadResult uploadFileSilent(String fileName, int localId) throws Exception {
        int taskId = application.getApi().getUploader().requestTask(fileName, new UploadListener() {
            @Override
            public void onPartUploaded(int percent, int downloadedSize) {

            }
        });
        SendState state = states.get(localId);
        state.currentUploadId = taskId;
        application.getApi().getUploader().waitForTask(taskId);
        Uploader.UploadResult result = application.getApi().getUploader().getUploadResult(taskId);
        if (result == null) {
            throw new IOException("Unable to upload file");
        }

        return result;
    }

    private VideoMetadata getVideoMetadata(String fileName) throws Exception {
        long timeInmillisec;
        int width;
        int height;
        Bitmap img;

        if (Build.VERSION.SDK_INT >= 10) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(fileName);
                timeInmillisec = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                img = retriever.getFrameAtTime(0);
                width = img.getWidth();
                height = img.getHeight();
            } catch (Exception e) {
                CrashHandler.logHandledException(e);
                throw e;
            }
        } else {
            img = ThumbnailUtils.createVideoThumbnail(fileName,
                    MediaStore.Images.Thumbnails.MINI_KIND);

            MediaPlayer mp = new MediaPlayer();
            final Object locker = new Object();
            final int[] sizes = new int[2];
            try {
                mp.setDataSource(fileName);
                mp.prepare();
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                        synchronized (locker) {
                            sizes[0] = width;
                            sizes[1] = height;
                            locker.notify();
                        }
                    }
                });

                synchronized (locker) {
                    if (sizes[0] == 0 || sizes[1] == 1) {
                        locker.wait(5000);
                    }
                }

                if (sizes[0] == 0 || sizes[1] == 1) {
                    throw new IOException();
                }

                timeInmillisec = mp.getDuration() * 1000L;
                width = sizes[0];
                height = sizes[1];
            } catch (Exception e) {
                CrashHandler.logHandledException(e);
                throw e;
            }
        }

        return new VideoMetadata(timeInmillisec, width, height, img);
    }

    private EncryptedFile encryptFile(String fileName) throws Exception {
        String resFile = getUploadTempFile();
        byte[] iv = Entropy.generateSeed(32);
        byte[] key = Entropy.generateSeed(32);
        CryptoUtils.AES256IGEEncrypt(new File(fileName), new File(resFile), iv, key);
        return new EncryptedFile(resFile, iv, key);
    }


    public class SendState {
        private boolean isSent;
        private boolean isUploaded;
        private int uploadProgress;
        private boolean isCanceled;
        private int currentUploadId;

        public boolean isCanceled() {
            return isCanceled;
        }

        public boolean isSent() {
            return isSent;
        }

        public boolean isUploaded() {
            return isUploaded;
        }

        public int getUploadProgress() {
            return uploadProgress;
        }
    }

    private class EncryptedDocSent {
        private TLAbsSentEncryptedMessage encryptedMessage;
        private TLLocalDocument document;

        private EncryptedDocSent(TLAbsSentEncryptedMessage encryptedMessage, TLLocalDocument document) {
            this.encryptedMessage = encryptedMessage;
            this.document = document;
        }

        public TLAbsSentEncryptedMessage getEncryptedMessage() {
            return encryptedMessage;
        }

        public TLLocalDocument getDocument() {
            return document;
        }
    }

    private class EncryptedVideoSent {
        private TLAbsSentEncryptedMessage encryptedMessage;
        private TLLocalVideo video;

        private EncryptedVideoSent(TLAbsSentEncryptedMessage encryptedMessage, TLLocalVideo video) {
            this.encryptedMessage = encryptedMessage;
            this.video = video;
        }

        public TLAbsSentEncryptedMessage getEncryptedMessage() {
            return encryptedMessage;
        }

        public TLLocalVideo getVideo() {
            return video;
        }
    }

    private class EncryptedPhotoSent {
        private TLAbsSentEncryptedMessage encryptedMessage;
        private TLLocalPhoto photo;

        private EncryptedPhotoSent(TLAbsSentEncryptedMessage encryptedMessage, TLLocalPhoto photo) {
            this.encryptedMessage = encryptedMessage;
            this.photo = photo;
        }

        public TLAbsSentEncryptedMessage getEncryptedMessage() {
            return encryptedMessage;
        }

        public TLLocalPhoto getPhoto() {
            return photo;
        }
    }

    private class EncryptedFile {
        private String fileName;
        private byte[] iv;
        private byte[] key;

        private EncryptedFile(String fileName, byte[] iv, byte[] key) {
            this.fileName = fileName;
            this.iv = iv;
            this.key = key;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getIv() {
            return iv;
        }

        public byte[] getKey() {
            return key;
        }
    }

    private class VideoMetadata {
        private long duration;
        private int width;
        private int height;
        private Bitmap img;

        private VideoMetadata(long duration, int width, int height, Bitmap img) {
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.img = img;
        }

        public long getDuration() {
            return duration;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Bitmap getImg() {
            return img;
        }
    }
}
