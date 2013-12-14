package org.telegram.android.core.background;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.*;
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
import org.telegram.api.engine.RpcException;
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

    public class SendState {
        private boolean isSent;
        private boolean isUploaded;
        private int uploadProgress;
        private boolean isCanceled;

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

    private Handler handler = new Handler(Looper.getMainLooper());

    private HashMap<Integer, SendState> states = new HashMap<Integer, SendState>();

    private static final int TIMEOUT = 5 * 60 * 1000;

    private Executor mediaExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageMediaSender#" + res.hashCode());
            return res;
        }
    });

    private StelsApplication application;

    private CopyOnWriteArrayList<WeakReference<SenderListener>> listeners = new CopyOnWriteArrayList<WeakReference<SenderListener>>();

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

    public MediaSender(StelsApplication application) {
        this.application = application;
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

    private Uploader.UploadResult uploadFile(String fileName, final int localId) throws Exception {
        int taskId = application.getApi().getUploader().requestTask(fileName, new UploadListener() {
            @Override
            public void onPartUploaded(int percent, int downloadedSize) {
                SendState state = states.get(localId);
                state.uploadProgress = percent;
                updateState(localId, state);
            }
        });
        application.getApi().getUploader().waitForTask(taskId);
        Uploader.UploadResult result = application.getApi().getUploader().getUploadResult(taskId);
        if (result == null) {
            throw new IOException("Unable to upload file");
        }

        return result;
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

    private void completePhotoSending(TLAbsStatedMessage sent, String uploadFileName, ChatMessage message) {
        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(uploadFileName);
        application.getUpdateProcessor().onMessage(new TLLocalMessageSentPhoto(sent, message,
                previewResult.getData(), previewResult.getW(), previewResult.getH()));
    }

    private void uploadPhoto(ChatMessage message) throws Exception {
        String uploadFileName = preparePhoto(message);
        Uploader.UploadResult result = uploadFile(uploadFileName, message.getDatabaseId());
        TLAbsStatedMessage sent = doSendPhoto(result, message);
        saveUploadedPhoto(sent, uploadFileName);
        completePhotoSending(sent, uploadFileName, message);
    }

    public void sendMedia(final ChatMessage message) {
        SendState sendState = new SendState();
        sendState.isSent = false;
        sendState.isUploaded = false;
        sendState.uploadProgress = 0;
        states.put(message.getDatabaseId(), sendState);
        updateState(message.getDatabaseId(), sendState);
        final long start = SystemClock.uptimeMillis();
        mediaExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                if (message.getPeerType() != PeerType.PEER_USER_ENCRYPTED && message.getExtras() instanceof TLUploadingPhoto) {
                    try {
                        uploadPhoto(message);
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
                        application.notifyUIUpdate();
                    }
                    return;
                }
                while (SystemClock.uptimeMillis() - start < TIMEOUT) {
                    synchronized (states) {
                        SendState state = states.get(message.getDatabaseId());
                        if (state.isCanceled) {
                            return;
                        }
                        state.isUploaded = false;
                    }

                    TLAbsInputPeer peer = null;
                    if (message.getPeerType() == PeerType.PEER_USER) {
                        User user = application.getEngine().getUser(message.getPeerId());
                        peer = new TLInputPeerForeign(user.getUid(), user.getAccessHash());
                    } else {
                        peer = new TLInputPeerChat(message.getPeerId());
                    }

                    if (message.getExtras() instanceof TLUploadingPhoto) {
                        Uploader.UploadResult result;
                        TLUploadingPhoto uploadingPhoto = (TLUploadingPhoto) message.getExtras();
                        FileInputStream fileInputStream = null;
                        String destFile = null;
                        String originalFile = null;
                        int originalLen = 0;
                        byte[] iv = null;
                        byte[] key = null;
                        try {
                            destFile = getUploadTempFile();
                            originalFile = destFile;
                            if (uploadingPhoto.getFileUri() != null && uploadingPhoto.getFileUri().length() > 0) {
                                Optimizer.optimize(uploadingPhoto.getFileUri(), application, destFile);
                            } else {
                                Optimizer.optimize(uploadingPhoto.getFileName(), destFile);
                            }

                            File file = new File(destFile);
                            originalLen = (int) file.length();
                            if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                iv = Entropy.generateSeed(32);
                                key = Entropy.generateSeed(32);

                                File destEncryptedFile = new File(getUploadTempFile());
                                long start = SystemClock.uptimeMillis();
                                CryptoUtils.AES256IGEEncrypt(file, destEncryptedFile, iv, key);
                                Logger.d(TAG, "encryption time: " + (SystemClock.uptimeMillis() - start) + " ms");
                                file = destEncryptedFile;
                            }

                            int taskId = application.getApi().getUploader().requestTask(file.getAbsolutePath(), new UploadListener() {
                                @Override
                                public void onPartUploaded(int percent, int downloadedSize) {
                                    SendState state = states.get(message.getDatabaseId());
                                    state.uploadProgress = percent;
                                    updateState(message.getDatabaseId(), state);
                                }
                            });
                            application.getApi().getUploader().waitForTask(taskId);
                            result = application.getApi().getUploader().getUploadResult(taskId);

                            if (result == null)
                                break;
                        } catch (Exception e) {
                            continue;
                        } finally {
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e) {
                                    Logger.t(TAG, e);
                                }
                            }
                        }
                        synchronized (states) {
                            SendState state = states.get(message.getDatabaseId());
                            if (state.isCanceled) {
                                return;
                            }
                            state.isUploaded = true;
                            updateState(message.getDatabaseId(), state);
                        }

                        if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                            EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());
                            TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
                            decryptedMessage.setRandomId(message.getRandomId());
                            decryptedMessage.setRandomBytes(Entropy.generateSeed(32));
                            decryptedMessage.setMessage("");
                            final Point size;
                            try {
                                size = Optimizer.getSize(originalFile);
                            } catch (IOException e) {
                                Logger.t(TAG, e);
                                return;
                            }

                            Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(originalFile);

                            decryptedMessage.setMedia(new TLDecryptedMessageMediaPhoto(previewResult.getData(),
                                    previewResult.getW(), previewResult.getH(), size.x, size.y, originalLen, key, iv));
                            byte[] digest = MD5Raw(concat(key, iv));
                            int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)), 0);
                            try {
                                byte[] bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());

                                TLAbsInputEncryptedFile inputEncryptedFile;
                                if (result.isUsedBigFile()) {
                                    inputEncryptedFile = new TLInputEncryptedFileBigUploaded(result.getFileId(), result.getPartsCount(), fingerprint);
                                } else {
                                    inputEncryptedFile = new TLInputEncryptedFileUploaded(result.getFileId(), result.getPartsCount(), result.getHash(), fingerprint);
                                }

                                TLAbsSentEncryptedMessage encryptedMessage =
                                        application.getApi().doRpcCall(new TLRequestMessagesSendEncryptedFile(
                                                new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(), bundle,
                                                inputEncryptedFile));

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
                                        photo.setFullLocation(new TLLocalEncryptedFileLocation(file1.getId(), file1.getAccessHash(), file1.getSize(), file1.getDcId(), key, iv));
                                    } else {
                                        photo.setFullLocation(new TLLocalFileEmpty());
                                    }
                                } else {
                                    photo.setFullLocation(new TLLocalFileEmpty());
                                }

                                if (photo.getFullLocation() instanceof TLLocalEncryptedFileLocation) {
                                    try {
                                        String downloadKey = DownloadManager.getPhotoKey(photo);
                                        application.getDownloadManager().saveDownloadImage(downloadKey, originalFile);
                                        if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                                            application.getDownloadManager().writeToGallery(destFile, downloadKey + ".jpg");
                                        }
                                    } catch (IOException e) {
                                        Logger.t(TAG, e);
                                    }
                                }

                                message.setMessageTimeout(chat.getSelfDestructTime());
                                application.getEngine().onMessageEncPhotoSent(message, encryptedMessage.getDate(), photo);
                            } catch (IOException e) {
                                Logger.t(TAG, e);
                                return;
                            }

                            application.notifyUIUpdate();

                            synchronized (states) {
                                SendState state = states.get(message.getDatabaseId());
                                state.isSent = true;
                                updateState(message.getDatabaseId(), state);
                            }

                            return;
                        } else {
                            try {

                                TLAbsInputFile inputFile;
                                if (result.isUsedBigFile()) {
                                    inputFile = new TLInputFileBig(result.getFileId(), result.getPartsCount(), "file.jpg");
                                } else {
                                    inputFile = new TLInputFile(result.getFileId(), result.getPartsCount(), "file.jpg", result.getHash());
                                }

                                TLAbsStatedMessage sent = application.getApi().doRpcCall(
                                        new TLRequestMessagesSendMedia(peer, new TLInputMediaUploadedPhoto(inputFile), message.getRandomId()));
                                // application.getUpdateProcessor().onMessage(sent);
                                try {
                                    TLMessage msgRes = (TLMessage) sent.getMessage();
                                    TLLocalPhoto mediaPhoto = EngineUtils.convertPhoto((TLMessageMediaPhoto) msgRes.getMedia());
                                    if (!(mediaPhoto.getFullLocation() instanceof TLLocalFileEmpty)) {
                                        String downloadKey = DownloadManager.getPhotoKey(mediaPhoto);
                                        application.getDownloadManager().saveDownloadImage(downloadKey, destFile);
                                        if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                                            application.getDownloadManager().writeToGallery(destFile, downloadKey + ".jpg");
                                        }
                                    }
                                } catch (Exception e) {
                                    Logger.t(TAG, e);
                                }

                                synchronized (states) {
                                    SendState state = states.get(message.getDatabaseId());
                                    state.isSent = true;
                                    updateState(message.getDatabaseId(), state);
                                }

                                application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));

                                return;
                            } catch (RpcException e) {
                                Logger.t(TAG, e);
                                break;
                            } catch (IOException e) {
                                Logger.t(TAG, e);
                                break;
                            }
                        }
                    } else if (message.getExtras() instanceof TLUploadingVideo) {
                        TLUploadingVideo uploadingVideo = (TLUploadingVideo) message.getExtras();
                        Uploader.UploadResult result;
                        FileInputStream fileInputStream = null;
                        byte[] key = null;
                        byte[] iv = null;
                        try {
                            final File file;
                            if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
                                File srcFile = new File(uploadingVideo.getFileName());
                                File destFile = new File(getUploadTempFile());
                                key = Entropy.generateSeed(32);
                                iv = Entropy.generateSeed(32);
                                AES256IGEEncrypt(srcFile, destFile, iv, key);
                                file = destFile;
                            } else {
                                file = new File(uploadingVideo.getFileName());
                            }
                            fileInputStream = new FileInputStream(file);

                            int taskId = application.getApi().getUploader().requestTask(file.getAbsolutePath(), new UploadListener() {
                                @Override
                                public void onPartUploaded(int percent, int downloadedSize) {
                                    SendState state = states.get(message.getDatabaseId());
                                    state.uploadProgress = percent;
                                    updateState(message.getDatabaseId(), state);
                                }
                            });
                            application.getApi().getUploader().waitForTask(taskId);
                            result = application.getApi().getUploader().getUploadResult(taskId);

//                            result = application.getUploadController()
//                                    .uploadFile(fileInputStream, (int) file.length(), fileId,
//                                            new UploadController.UploadListener() {
//                                                @Override
//                                                public void onProgressChanged(int percent) {
//                                                    SendState state = states.get(message.getDatabaseId());
//                                                    if (state.isCanceled) {
//                                                        throw new RuntimeException();
//                                                    }
//                                                    state.uploadProgress = percent;
//                                                    Logger.d(TAG, "percent: " + percent);
//                                                    updateState(message.getDatabaseId(), state);
//                                                }
//                                            });
                            if (result == null)
                                continue;
                        } catch (Exception e) {
                            continue;
                        } finally {
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e) {
                                    Logger.t(TAG, e);
                                }
                            }
                        }

                        long timeInmillisec;
                        int width;
                        int height;
                        Bitmap img;
                        try {
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(uploadingVideo.getFileName());
                            timeInmillisec = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                            img = retriever.getFrameAtTime(0);
                            width = img.getWidth();
                            height = img.getHeight();
                        } catch (Exception e) {
                            Logger.t(TAG, e);
                            break;
                        }

                        if (img == null) {
                            break;
                        }

                        Optimizer.FastPreviewResult previewResult = Optimizer.buildPreview(img);
                        if (previewResult == null) {
                            break;
                        }

                        if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {

                            synchronized (states) {
                                SendState state = states.get(message.getDatabaseId());
                                if (state.isCanceled) {
                                    return;
                                }
                                state.isUploaded = true;
                                updateState(message.getDatabaseId(), state);
                            }


                            EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());

                            TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
                            decryptedMessage.setRandomId(message.getRandomId());
                            decryptedMessage.setRandomBytes(Entropy.generateSeed(32));
                            decryptedMessage.setMessage("");
                            TLDecryptedMessageMediaVideo video = new TLDecryptedMessageMediaVideo();
                            video.setDuration((int) (timeInmillisec / 1000));
                            video.setH(height);
                            video.setW(width);
                            video.setSize((int) new File(uploadingVideo.getFileName()).length());
                            video.setThumbW(previewResult.getW());
                            video.setThumbH(previewResult.getH());
                            video.setThumb(previewResult.getData());
                            video.setKey(key);
                            video.setIv(iv);
                            decryptedMessage.setMedia(video);

                            byte[] digest = MD5Raw(concat(key, iv));
                            int fingerprint = StreamingUtils.readInt(xor(substring(digest, 0, 4), substring(digest, 4, 4)), 0);

                            try {
                                byte[] bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());

                                TLAbsInputEncryptedFile inputFile;
                                if (result.isUsedBigFile()) {
                                    inputFile = new TLInputEncryptedFileBigUploaded(result.getFileId(), result.getPartsCount(), fingerprint);
                                } else {
                                    inputFile = new TLInputEncryptedFileUploaded(result.getFileId(), result.getPartsCount(), result.getHash(), fingerprint);
                                }

                                TLAbsSentEncryptedMessage encryptedMessage = application.getApi().doRpcCall(
                                        new TLRequestMessagesSendEncryptedFile(
                                                new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), message.getRandomId(), bundle, inputFile));

                                TLLocalVideo localVideo = new TLLocalVideo();
                                localVideo.setDuration((int) (timeInmillisec / 1000));
                                localVideo.setPreviewW(previewResult.getW());
                                localVideo.setPreviewH(previewResult.getH());
                                localVideo.setFastPreview(previewResult.getData());
                                localVideo.setPreviewKey(decryptedMessage.getRandomId() + "_video");
                                localVideo.setPreviewLocation(new TLLocalFileEmpty());

                                if (encryptedMessage instanceof TLSentEncryptedFile) {
                                    TLSentEncryptedFile file = (TLSentEncryptedFile) encryptedMessage;
                                    if (file.getFile() instanceof TLEncryptedFile) {
                                        TLEncryptedFile file1 = (TLEncryptedFile) file.getFile();
                                        localVideo.setVideoLocation(new TLLocalEncryptedFileLocation(file1.getId(), file1.getAccessHash(), file1.getSize(), file1.getDcId(), key, iv));
                                    } else {
                                        localVideo.setVideoLocation(new TLLocalFileEmpty());
                                    }
                                } else {
                                    localVideo.setVideoLocation(new TLLocalFileEmpty());
                                }

                                if (localVideo.getVideoLocation() instanceof TLLocalEncryptedFileLocation) {
                                    try {
                                        String downloadKey = DownloadManager.getVideoKey(localVideo);
                                        application.getDownloadManager().saveDownloadVideo(downloadKey, uploadingVideo.getFileName());
                                        if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                                            application.getDownloadManager().writeToGallery(uploadingVideo.getFileName(), downloadKey + ".mp4");
                                        }
                                    } catch (IOException e) {
                                        Logger.t(TAG, e);
                                    }
                                }

                                message.setMessageTimeout(chat.getSelfDestructTime());
                                application.getEngine().onMessageVideoSent(message, encryptedMessage.getDate(), localVideo);

                                application.notifyUIUpdate();

                                synchronized (states) {
                                    SendState state = states.get(message.getDatabaseId());
                                    state.isSent = true;
                                    updateState(message.getDatabaseId(), state);
                                }

                                return;
                            } catch (RpcException e) {
                                Logger.t(TAG, e);
                                break;
                            } catch (IOException e) {
                                Logger.t(TAG, e);
                                break;
                            }
                        } else {
                            Uploader.UploadResult thumbResult;
                            String previewFile = getUploadTempFile();
                            try {
                                RandomAccessFile thumbFile = new RandomAccessFile(previewFile, "rw");
                                thumbFile.write(previewResult.getData());
                                thumbFile.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            int taskId = application.getApi().getUploader().requestTask(previewFile, new UploadListener() {
                                @Override
                                public void onPartUploaded(int percent, int downloadedSize) {
                                    SendState state = states.get(message.getDatabaseId());
                                    state.uploadProgress = percent;
                                    updateState(message.getDatabaseId(), state);
                                }
                            });
                            application.getApi().getUploader().waitForTask(taskId);
                            thumbResult = application.getApi().getUploader().getUploadResult(taskId);

//                            UploadResult thumbResult;
//                            try {
//                                thumbResult = application.getUploadController()
//                                        .uploadFile(new ByteArrayInputStream(previewResult.getData()), previewResult.getData().length, thumbFileId,
//                                                new UploadController.UploadListener() {
//                                                    @Override
//                                                    public void onProgressChanged(int percent) {
//                                                    }
//                                                });
//                                if (thumbResult == null)
//                                    continue;
//                            } catch (Exception e) {
//                                continue;
//                            }

                            synchronized (states) {
                                SendState state = states.get(message.getDatabaseId());
                                if (state.isCanceled) {
                                    return;
                                }
                                state.isUploaded = true;
                                updateState(message.getDatabaseId(), state);
                            }
                            try {
                                TLInputMediaUploadedThumbVideo video = new TLInputMediaUploadedThumbVideo();
                                //TLInputMediaUploadedVideo video = new TLInputMediaUploadedVideo();
                                if (result.isUsedBigFile()) {
                                    video.setFile(new TLInputFileBig(result.getFileId(), result.getPartsCount(), "file.mp4"));
                                } else {
                                    video.setFile(new TLInputFile(result.getFileId(), result.getPartsCount(), "file.mp4", result.getHash()));
                                }
                                video.setThumb(new TLInputFile(thumbResult.getFileId(), thumbResult.getPartsCount(), "file.jpg", thumbResult.getHash()));
                                video.setDuration((int) (timeInmillisec / 1000));
                                video.setW(width);
                                video.setH(height);

                                TLAbsStatedMessage sent = application.getApi().doRpcCall(new TLRequestMessagesSendMedia(peer, video, message.getRandomId()));
                                try {
                                    TLMessage msgRes = (TLMessage) sent.getMessage();
                                    TLLocalVideo mediaVideo = EngineUtils.convertVideo((TLMessageMediaVideo) msgRes.getMedia());
                                    String downloadKey = DownloadManager.getVideoKey(mediaVideo);
                                    application.getDownloadManager().saveDownloadVideo(downloadKey, uploadingVideo.getFileName());
                                    if (application.getSettingsKernel().getUserSettings().isSaveToGalleryEnabled()) {
                                        application.getDownloadManager().writeToGallery(uploadingVideo.getFileName(), key + ".mp4");
                                    }
                                } catch (Exception e) {
                                    Logger.t(TAG, e);
                                }
                                application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));
                                return;
                            } catch (RpcException e) {
                                Logger.t(TAG, e);
                                break;
                            } catch (IOException e) {
                                Logger.t(TAG, e);
                                break;
                            }
                        }
                    } else if (message.getExtras() instanceof TLUploadingDocument) {

                    }
                }
                synchronized (states) {
                    SendState state = states.get(message.getDatabaseId());
                    if (state.isCanceled) {
                        return;
                    }
                }
                application.getEngine().onMessageFailure(message);
                application.notifyUIUpdate();
            }
        });
    }
}
