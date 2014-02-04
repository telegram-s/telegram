package org.telegram.android.media;

import android.graphics.*;
import android.media.MediaScannerConnection;
import android.os.*;
import com.extradea.framework.images.BitmapDecoder;
import com.extradea.framework.images.utils.ImageUtils;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.*;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.UiNotifier;
import org.telegram.android.util.IOUtils;
import org.telegram.api.*;
import org.telegram.api.engine.file.Downloader;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 20:46
 */
public class DownloadManager {

    private static final String TAG = "Downloader";

    public static String getVideoKey(TLLocalVideo video) {
        String baseKey = "";
        if (video.getVideoLocation() instanceof TLLocalFileVideoLocation) {
            baseKey = ((TLLocalFileVideoLocation) video.getVideoLocation()).getDcId() + "_" + ((TLLocalFileVideoLocation) video.getVideoLocation()).getVideoId();
        } else if (video.getVideoLocation() instanceof TLLocalEncryptedFileLocation) {
            baseKey = ((TLLocalEncryptedFileLocation) video.getVideoLocation()).getDcId() + "_" + ((TLLocalEncryptedFileLocation) video.getVideoLocation()).getId();
        } else {
            throw new UnsupportedOperationException();
        }
        return "video_" + baseKey + ".mp4";
    }

    public static String getPhotoKey(TLLocalPhoto photo) {
        String baseKey = "";
        if (photo.getFullLocation() instanceof TLLocalFileLocation) {
            baseKey = ((TLLocalFileLocation) photo.getFullLocation()).getDcId() + "_" + ((TLLocalFileLocation) photo.getFullLocation()).getVolumeId() + "_" + ((TLLocalFileLocation) photo.getFullLocation()).getLocalId();
        } else if (photo.getFullLocation() instanceof TLLocalEncryptedFileLocation) {
            baseKey = ((TLLocalEncryptedFileLocation) photo.getFullLocation()).getDcId() + "_" + ((TLLocalEncryptedFileLocation) photo.getFullLocation()).getId();
        } else {
            throw new UnsupportedOperationException();
        }
        return "image_" + baseKey + ".jpg";
    }

    public static String getDocumentKey(TLLocalDocument document) {
        String baseKey = "";
        if (document.getFileLocation() instanceof TLLocalFileDocument) {
            baseKey = ((TLLocalFileDocument) document.getFileLocation()).getDcId() + "_" +
                    ((TLLocalFileDocument) document.getFileLocation()).getId() + "_" + document.getFileName();
        } else if (document.getFileLocation() instanceof TLLocalEncryptedFileLocation) {
            baseKey = ((TLLocalEncryptedFileLocation) document.getFileLocation()).getDcId() + "_" + ((TLLocalEncryptedFileLocation) document.getFileLocation()).getId() + document.getFileName();
        } else {
            throw new UnsupportedOperationException();
        }
        return "doc_" + baseKey;
    }

    public static String getAudioKey(TLLocalAudio audio) {
        String baseKey = "";
        if (audio.getFileLocation() instanceof TLLocalFileAudio) {
            baseKey = ((TLLocalFileAudio) audio.getFileLocation()).getDcId() + "_" +
                    ((TLLocalFileAudio) audio.getFileLocation()).getId();
        } else if (audio.getFileLocation() instanceof TLLocalEncryptedFileLocation) {
            baseKey = ((TLLocalEncryptedFileLocation) audio.getFileLocation()).getDcId() + "_" + ((TLLocalEncryptedFileLocation) audio.getFileLocation()).getId();
        } else {
            throw new UnsupportedOperationException();
        }
        return "audio_" + baseKey + ".m4a";
    }

    private final int SMALL_THUMB_SIDE;

    private HashSet<String> enqueued = new HashSet<String>();

    private class DownloadRecord {
        public TLLocalVideo video;
        public TLLocalPhoto photo;
        public TLLocalDocument doc;
        public TLLocalAudio audio;
        public int downloaded;
        public int downloadedPercent;
        public DownloadState state;
        public int downloadTask;
    }

    private ExecutorService service;

    private TelegramApplication application;

    private HashMap<String, DownloadRecord> records = new HashMap<String, DownloadRecord>();

    private CopyOnWriteArrayList<WeakReference<DownloadListener>> listeners = new CopyOnWriteArrayList<WeakReference<DownloadListener>>();

    private Handler handler = new Handler(Looper.getMainLooper());

    private FileCache fileCache;

    public DownloadManager(TelegramApplication application) {
        this.fileCache = new FileCache(application);
        this.service = Executors.newFixedThreadPool(3);
        this.application = application;

        final int margin = (int) (4 * application.getResources().getDisplayMetrics().density);
        SMALL_THUMB_SIDE = ((Math.min(application.getResources().getDisplayMetrics().widthPixels, application.getResources().getDisplayMetrics().heightPixels) - 5 * margin) / 4);
    }

    public void registerListener(DownloadListener listener) {
        for (WeakReference<DownloadListener> ref : listeners) {
            if (ref.get() == listener) {
                return;
            }
        }
        listeners.add(new WeakReference<DownloadListener>(listener));
    }

    public void unregisterListener(DownloadListener listener) {
        for (WeakReference<DownloadListener> ref : listeners) {
            if (ref.get() == listener) {
                listeners.remove(ref);
                return;
            }
        }
    }


    public DownloadState getState(String key) {
        if (fileCache.isDownloaded(key)) {
            return DownloadState.COMPLETED;
        }

        if (records.containsKey(key)) {
            DownloadRecord record = records.get(key);
            if (record.state == DownloadState.COMPLETED) {
                if (!fileCache.isDownloaded(key)) {
                    records.remove(key);
                    return DownloadState.NONE;
                }
            }
            return record.state;
        }

        return DownloadState.NONE;
    }

    public int getDownloadProgress(String key) {
        if (fileCache.isDownloaded(key)) {
            return 100;
        }

        if (records.containsKey(key)) {
            DownloadRecord record = records.get(key);
            return record.downloadedPercent;
        }

        return 0;
    }

    public void requestDownload(TLLocalDocument doc) {
        final String resourceKey = getDocumentKey(doc);

        if (fileCache.isDownloaded(resourceKey))
            return;

        final DownloadRecord record;
        if (records.containsKey(resourceKey)) {
            record = records.get(resourceKey);
            if (record.state == DownloadState.CANCELLED ||
                    record.state == DownloadState.FAILURE ||
                    record.state == DownloadState.NONE) {
                record.state = DownloadState.PENDING;
                record.downloaded = 0;
                record.downloadedPercent = 0;
                record.doc = doc;
                updateState(resourceKey, record.state, 0, 0, true);
            }
        } else {
            record = new DownloadRecord();
            record.state = DownloadState.PENDING;
            record.downloaded = 0;
            record.downloadedPercent = 0;
            record.doc = doc;
            records.put(resourceKey, record);
            updateState(resourceKey, record.state, 0, 0, true);
        }

        requestDownload(resourceKey, record,
                getFileName(resourceKey),
                doc.getFileLocation());
    }

    public void requestDownload(TLLocalAudio audio) {
        final String resourceKey = getAudioKey(audio);

        if (fileCache.isDownloaded(resourceKey))
            return;

        final DownloadRecord record;
        if (records.containsKey(resourceKey)) {
            record = records.get(resourceKey);
            if (record.state == DownloadState.CANCELLED ||
                    record.state == DownloadState.FAILURE ||
                    record.state == DownloadState.NONE) {
                record.state = DownloadState.PENDING;
                record.downloaded = 0;
                record.downloadedPercent = 0;
                record.audio = audio;
                updateState(resourceKey, record.state, 0, 0, true);
            }
        } else {
            record = new DownloadRecord();
            record.state = DownloadState.PENDING;
            record.downloaded = 0;
            record.downloadedPercent = 0;
            record.audio = audio;
            records.put(resourceKey, record);
            updateState(resourceKey, record.state, 0, 0, true);
        }

        requestDownload(resourceKey, record,
                getFileName(resourceKey),
                audio.getFileLocation());
    }


    public void requestDownload(TLLocalPhoto photo) {

        final String resourceKey = getPhotoKey(photo);

        if (fileCache.isDownloaded(resourceKey))
            return;

        final DownloadRecord record;
        if (records.containsKey(resourceKey)) {
            record = records.get(resourceKey);
            if (record.state == DownloadState.CANCELLED ||
                    record.state == DownloadState.FAILURE ||
                    record.state == DownloadState.NONE) {
                record.state = DownloadState.PENDING;
                record.downloaded = 0;
                record.downloadedPercent = 0;
                record.photo = photo;
                updateState(resourceKey, record.state, 0, 0, true);
            }
        } else {
            record = new DownloadRecord();
            record.state = DownloadState.PENDING;
            record.downloaded = 0;
            record.downloadedPercent = 0;
            record.photo = photo;
            records.put(resourceKey, record);
            updateState(resourceKey, record.state, 0, 0, true);
        }

        requestDownload(resourceKey, record,
                getFileName(resourceKey),
                photo.getFullLocation());
    }

    public void requestDownload(TLLocalVideo video) {
        final String resourceKey = getVideoKey(video);

        if (fileCache.isDownloaded(resourceKey))
            return;

        final DownloadRecord record;
        if (records.containsKey(resourceKey)) {
            record = records.get(resourceKey);
            if (record.state == DownloadState.CANCELLED ||
                    record.state == DownloadState.FAILURE ||
                    record.state == DownloadState.NONE) {
                record.state = DownloadState.PENDING;
                record.downloaded = 0;
                record.downloadedPercent = 0;
                record.video = video;
                updateState(resourceKey, record.state, 0, 0, true);
            }
        } else {
            record = new DownloadRecord();
            record.state = DownloadState.PENDING;
            record.downloaded = 0;
            record.downloadedPercent = 0;
            record.video = video;
            records.put(resourceKey, record);
            updateState(resourceKey, record.state, 0, 0, true);
        }

        requestDownload(resourceKey, record,
                getFileName(resourceKey),
                video.getVideoLocation());
    }

    private void requestDownload(final String key, final DownloadRecord record, final String fileName, final TLAbsLocalFileLocation fileLocation) {
        if (enqueued.contains(key)) {
            return;
        }
        enqueued.add(key);

        final TLAbsInputFileLocation location;
        final int dcId;
        final int size;
        if (fileLocation instanceof TLLocalFileVideoLocation) {
            dcId = ((TLLocalFileVideoLocation) fileLocation).getDcId();
            size = ((TLLocalFileVideoLocation) fileLocation).getSize();
            location = new TLInputVideoFileLocation(((TLLocalFileVideoLocation) fileLocation).getVideoId(), ((TLLocalFileVideoLocation) fileLocation).getAccessHash());
        } else if (fileLocation instanceof TLLocalFileLocation) {
            dcId = ((TLLocalFileLocation) fileLocation).getDcId();
            size = ((TLLocalFileLocation) fileLocation).getSize();
            location = new TLInputFileLocation(((TLLocalFileLocation) fileLocation).getVolumeId(), ((TLLocalFileLocation) fileLocation).getLocalId(), ((TLLocalFileLocation) fileLocation).getSecret());
        } else if (fileLocation instanceof TLLocalEncryptedFileLocation) {
            dcId = ((TLLocalEncryptedFileLocation) fileLocation).getDcId();
            size = ((TLLocalEncryptedFileLocation) fileLocation).getSize();
            location = new TLInputEncryptedFileLocation(((TLLocalEncryptedFileLocation) fileLocation).getId(), ((TLLocalEncryptedFileLocation) fileLocation).getAccessHash());
        } else if (fileLocation instanceof TLLocalFileDocument) {
            dcId = ((TLLocalFileDocument) fileLocation).getDcId();
            size = ((TLLocalFileDocument) fileLocation).getSize();
            location = new TLInputDocumentFileLocation(((TLLocalFileDocument) fileLocation).getId(), ((TLLocalFileDocument) fileLocation).getAccessHash());
        } else {
            return;
        }

        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    updateState(key, DownloadState.IN_PROGRESS, 0, 0, true);

                    String destFileName = getDownloadTempFile();

                    record.downloadTask = application.getApi().getDownloader().requestTask(dcId, location, size, destFileName, new org.telegram.api.engine.file.DownloadListener() {
                        @Override
                        public void onPartDownloaded(int percent, int downloadedSize) {
                            if (record.state != DownloadState.CANCELLED) {
                                updateState(key, DownloadState.IN_PROGRESS, (percent * 90) / 100, downloadedSize, false);
                            }
                        }

                        @Override
                        public void onDownloaded() {

                        }

                        @Override
                        public void onFailed() {

                        }
                    });

                    Logger.d(TAG, "Waiting for task #" + record.downloadTask);

                    application.getApi().getDownloader().waitForTask(record.downloadTask);

                    Logger.d(TAG, "Waiting for task ended #" + record.downloadTask);

                    if (application.getApi().getDownloader().getTaskState(record.downloadTask) != Downloader.FILE_COMPLETED) {
                        updateState(key, DownloadState.FAILURE, 0, 0, false);
                        return;
                    }

                    try {
                        if (fileLocation instanceof TLLocalEncryptedFileLocation) {
                            byte[] iv = ((TLLocalEncryptedFileLocation) fileLocation).getIv();
                            byte[] key = ((TLLocalEncryptedFileLocation) fileLocation).getKey();
                            Logger.d(TAG, "Decrypting file #" + record.downloadTask);
                            CryptoUtils.AES256IGEDecrypt(new File(destFileName), new File(fileName), iv, key);
                            IOUtils.delete(new File(destFileName));
                            Logger.d(TAG, "Decrypting file end #" + record.downloadTask);
                        } else {
                            Logger.d(TAG, "Copying file #" + record.downloadTask);
                            IOUtils.copy(new File(destFileName), new File(fileName));
                            IOUtils.delete(new File(destFileName));
                            Logger.d(TAG, "Copying file end #" + record.downloadTask);
                        }
                    } catch (IOException e) {
                        Logger.t(TAG, e);
                        updateState(key, DownloadState.FAILURE, 0, 0, true);
                        return;
                    }

                    if (record.photo != null) {
                        try {
                            Logger.d(TAG, "Saving preview #" + record.downloadTask);
                            saveDownloadImagePreview(key, fileName);
                            Logger.d(TAG, "Saving end #" + record.downloadTask);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Logger.t(TAG, e);
                            updateState(key, DownloadState.FAILURE, 0, 0, true);
                            return;
                        }

                        try {
                            Logger.d(TAG, "Saving to gallery #" + record.downloadTask);
                            if (application.getUserSettings().isSaveToGalleryEnabled()) {
                                writeToGallery(fileName, key + ".jpg");
                                Logger.d(TAG, "Saving to gallery end #" + record.downloadTask);
                            }
                        } catch (Exception e) {
                            Logger.t(TAG, e);
                        }
                    } else if (record.video != null) {
                        Logger.d(TAG, "@" + key + " = writing to gallery");
                        if (application.getUserSettings().isSaveToGalleryEnabled()) {
                            try {
                                writeToGallery(fileName, key + ".mp4");
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                            }
                        }
                    } else if (record.doc != null) {

                    } else if (record.audio != null) {

                    }
                    Logger.d(TAG, "@" + key + " = mark as downloaded");
                    fileCache.trackFile(key);
                    updateState(key, DownloadState.COMPLETED, 100, size, true);
                } finally {
                    enqueued.remove(key);
                }
            }
        });
    }

    public void abortDownload(String key) {
        DownloadRecord record = records.get(key);
        if (record != null) {
            updateState(key, DownloadState.CANCELLED, record.downloadedPercent, record.downloaded, true);
            application.getApi().getDownloader().cancelTask(record.downloadTask);
        }
    }

    public void saveDownloadImage(String key, String fileName) throws IOException {
        IOUtils.copy(new File(fileName), new File(getFileName(key)));
        fileCache.trackFile(key);
    }

    public void saveDownloadImagePreview(String key, final String fileName) throws IOException {
        Bitmap thumb = application.getImageController().getBitmapDecoder().executeGuarded(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                return ImageUtils.getBitmapThumb(fileName, SMALL_THUMB_SIDE, SMALL_THUMB_SIDE);
            }
        });
        if (thumb == null) {
            updateState(key, DownloadState.FAILURE, 0, 0, true);
            return;
        }

        Logger.d(TAG, "@" + key + " = saving thumb");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(getPreviewFileName(key));
            thumb.compress(Bitmap.CompressFormat.JPEG, 87, outputStream);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    public void saveDownloadVideo(String key, String fileName) throws IOException {
        IOUtils.copy(new File(fileName), new File(getFileName(key)));
        fileCache.trackFile(key);
    }

    public void saveDownloadDoc(String key, String fileName) throws IOException {
        IOUtils.copy(new File(fileName), new File(getFileName(key)));
        fileCache.trackFile(key);
    }

    public void saveDownloadAudio(String key, String fileName) throws IOException {
        IOUtils.copy(new File(fileName), new File(getFileName(key)));
        fileCache.trackFile(key);
    }

    public void writeToGallery(String fileName, String destName) throws IOException {
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                + "/Telegram/";
        new File(directory).mkdirs();
        String file = directory + destName;
        File dest = new File(file);
        if (dest.exists()) {
            return;
        }
        IOUtils.copy(new File(fileName), dest);
        MediaScannerConnection.scanFile(application, new String[]{file}, null, null);
    }

    public String getFileName(String key) {
        return fileCache.getFullPath(key);
    }

    public String getPreviewFileName(String key) {
        return fileCache.getFullPath("thumb_" + key);
    }

    private String getDownloadTempFile() {
        return fileCache.getFullPath("download_" + Entropy.generateRandomId() + ".bin");
    }


    private void updateState(final String key, final DownloadState state, final int percent, int bytes, boolean ignoreCancel) {
        Logger.d(TAG, "State: " + key + " = " + state + " " + percent + "%");

        DownloadRecord record = records.get(key);

        if (!ignoreCancel && record.state == DownloadState.CANCELLED) {
            return;
        }

        record.state = state;
        record.downloadedPercent = percent;
        record.downloaded = bytes;

        handler.post(new Runnable() {
            @Override
            public void run() {
                for (WeakReference<DownloadListener> ref : listeners) {
                    DownloadListener listener = ref.get();
                    if (listener != null) {
                        listener.onStateChanged(key, state, percent);
                    }
                }
            }
        });
    }

    public void clear() {
        fileCache.clearCache();
    }
}