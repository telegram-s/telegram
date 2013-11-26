package org.telegram.android.core.background;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.*;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.EngineUtils;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.core.model.media.*;
import org.telegram.android.core.model.update.TLLocalMessageEncryptedSent;
import org.telegram.android.core.model.update.TLLocalMessageSent;
import org.telegram.android.core.model.update.TLLocalMessageSentStated;
import org.telegram.android.core.model.update.TLLocalMessagesSentStated;
import org.telegram.android.log.Logger;
import org.telegram.android.media.Optimizer;
import org.telegram.android.core.model.*;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.EmojiProcessor;
import org.telegram.android.ui.LastEmojiProcessor;
import org.telegram.api.*;
import org.telegram.api.TLMessage;
import org.telegram.api.engine.RpcCallback;
import org.telegram.api.engine.RpcCallbackEx;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.file.UploadListener;
import org.telegram.api.engine.file.Uploader;
import org.telegram.api.messages.*;
import org.telegram.api.requests.*;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.tl.StreamingUtils;
import org.telegram.tl.TLIntVector;
import org.telegram.tl.TLMethod;
import org.telegram.tl.TLObject;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.telegram.mtproto.secure.CryptoUtils.*;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 2:02
 */
public class MessageSender {

    private static final String TAG = "Uploader";

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

    private Executor mediaExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageMediaSender#" + res.hashCode());
            return res;
        }
    });

    private Executor senderExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("MessageSender#" + res.hashCode());
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

    public MessageSender(StelsApplication application) {
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
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Logger.t(TAG, e);
                    return;
                }
                long id = Entropy.generateRandomId();
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
                        long fileId = Entropy.generateRandomId();
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

                            int taskId = application.getApi().getUploader().requestTask(fileId, file.getAbsolutePath(), new UploadListener() {
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
                                TLAbsSentEncryptedMessage encryptedMessage =
                                        application.getApi().doRpcCall(new TLRequestMessagesSendEncryptedFile(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), id, bundle,
                                                new TLInputEncryptedFileUploaded(fileId, result.getPartsCount(), result.getHash(), fingerprint)));

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
                                application.getEngine().onMessagePhotoSent(message, encryptedMessage.getDate(), photo);
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
                                TLAbsStatedMessage sent = application.getApi().doRpcCall(new TLRequestMessagesSendMedia(peer, new TLInputMediaUploadedPhoto(new TLInputFile(
                                        fileId, result.getPartsCount(), "file.jpg", result.getHash())), id));
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
                                application.getUpdateProcessor().onMessage(new TLLocalMessageSentStated(sent, message));

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
                        }
                    } else if (message.getExtras() instanceof TLUploadingVideo) {
                        TLUploadingVideo uploadingVideo = (TLUploadingVideo) message.getExtras();
                        long fileId = Entropy.generateRandomId();
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

                            int taskId = application.getApi().getUploader().requestTask(fileId, file.getAbsolutePath(), new UploadListener() {
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
                                TLAbsSentEncryptedMessage encryptedMessage = application.getApi().doRpcCall(new TLRequestMessagesSendEncryptedFile(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), id, bundle,
                                        new TLInputEncryptedFileUploaded(fileId, result.getPartsCount(), result.getHash(), fingerprint)));

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
                            long thumbFileId = Entropy.generateRandomId();
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
                            int taskId = application.getApi().getUploader().requestTask(thumbFileId, previewFile, new UploadListener() {
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
                                video.setFile(new TLInputFile(fileId, result.getPartsCount(), "file.mp4", result.getHash()));
                                video.setThumb(new TLInputFile(thumbFileId, thumbResult.getPartsCount(), "file.jpg", thumbResult.getHash()));
                                video.setDuration((int) (timeInmillisec / 1000));
                                video.setW(width);
                                video.setH(height);

                                TLAbsStatedMessage sent = application.getApi().doRpcCall(new TLRequestMessagesSendMedia(peer, video, id));
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

    public void sendMessage(final ChatMessage message) {
        if (message.getPeerType() == PeerType.PEER_USER_ENCRYPTED) {
            EncryptedChat chat = application.getEngine().getEncryptedChat(message.getPeerId());

            message.setMessageTimeout(chat.getSelfDestructTime());
            application.getEngine().getMessagesDao().update(message);
            application.getDataSourceKernel().onSourceUpdateMessage(message);

            TLDecryptedMessage decryptedMessage = new TLDecryptedMessage();
            decryptedMessage.setRandomId(message.getRandomId());
            decryptedMessage.setRandomBytes(Entropy.generateSeed(32));

            if (message.getContentType() == ContentType.MESSAGE_TEXT) {
                decryptedMessage.setMessage(message.getMessage());
                decryptedMessage.setMedia(new TLDecryptedMessageMediaEmpty());
            } else if (message.getContentType() == ContentType.MESSAGE_GEO) {
                TLLocalGeo point = (TLLocalGeo) message.getExtras();
                decryptedMessage.setMessage("");
                decryptedMessage.setMedia(new TLDecryptedMessageMediaGeoPoint(point.getLatitude(), point.getLongitude()));
            }

            byte[] bundle = new byte[0];
            try {
                bundle = application.getEncryptionController().encryptMessage(decryptedMessage, chat.getId());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            long randomId = EngineUtils.generateRandomId();
            TLRequestMessagesSendEncrypted request = new TLRequestMessagesSendEncrypted(new TLInputEncryptedChat(chat.getId(), chat.getAccessHash()), randomId, bundle);

            final long start = SystemClock.uptimeMillis();
            application.getApi().doRpcCall(request, TIMEOUT, new RpcCallbackEx<TLAbsSentEncryptedMessage>() {
                @Override
                public void onResult(TLAbsSentEncryptedMessage result) {
                    application.getUpdateProcessor().onMessage(new TLLocalMessageEncryptedSent(result, message));
                    Logger.d(TAG, "Chat message sent in time: " + (SystemClock.uptimeMillis() - start) + " ms");
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
