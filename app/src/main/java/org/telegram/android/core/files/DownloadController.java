package org.telegram.android.core.files;

import android.os.Build;
import android.os.Environment;
import android.util.Pair;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalEncryptedFileLocation;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalFileVideoLocation;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsInputFileLocation;
import org.telegram.api.TLInputEncryptedFileLocation;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.TLInputVideoFileLocation;
import org.telegram.api.upload.TLFile;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.Entropy;
import org.telegram.tl.TLObject;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 20:44
 */
public class DownloadController {

    private static final String TAG = "Download";

    public interface DownloadListener {
        public boolean onPieceDownloaded(int percent, int downloadedSize);
    }

    private static final int THREADS_COUNT = 2;

    private ExecutorService downloadService = Executors.newFixedThreadPool(THREADS_COUNT);

    private StelsApplication application;

    private static final int PAGE_SIZE = 128 * 1024;
    private static final int PAGE_SIZE_SLOW = 8 * 1024;

    private static final int TIMEOUT = 5 * 60 * 1000;

    public DownloadController(StelsApplication application) {
        this.application = application;
    }

    private String getDownloadTempFile() {
        if (Build.VERSION.SDK_INT >= 8) {
            try {
                //return getExternalCacheDir().getAbsolutePath();
                return ((File) StelsApplication.class.getMethod("getExternalCacheDir").invoke(application)).getAbsolutePath() + "/download_" + Entropy.generateRandomId() + ".bin";
            } catch (Exception e) {
                // Log.e(TAG, e);
            }
        }

        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/cache/" + application.getPackageName() + "/download_" + Entropy.generateRandomId() + ".bin";
    }

    public String downloadFile(TLAbsLocalFileLocation fileLocation, DownloadListener listener) {
        final int dcId;
        final int size;
        if (fileLocation instanceof TLLocalFileVideoLocation) {
            dcId = ((TLLocalFileVideoLocation) fileLocation).getDcId();
            size = ((TLLocalFileVideoLocation) fileLocation).getSize();
        } else if (fileLocation instanceof TLLocalFileLocation) {
            dcId = ((TLLocalFileLocation) fileLocation).getDcId();
            size = ((TLLocalFileLocation) fileLocation).getSize();
        } else if (fileLocation instanceof TLLocalEncryptedFileLocation) {
            dcId = ((TLLocalEncryptedFileLocation) fileLocation).getDcId();
            size = ((TLLocalEncryptedFileLocation) fileLocation).getSize();
        } else {
            return null;
        }

        String destFile = getDownloadTempFile();
        final FileOutputStream stream;
        try {
            stream = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            Logger.t(TAG, e);
            return null;
        }
        int requested = 0;
        int requestedCount = 0;
        int nextSaveBlock = 0;
        int nextDownloadBlock = 0;
        int downloaded = 0;
        HashMap<Integer, byte[]> downloadedBlocks = new HashMap<Integer, byte[]>();
        CopyOnWriteArrayList<Pair<Future<TLObject>, Integer>> active = new CopyOnWriteArrayList<Pair<Future<TLObject>, Integer>>();
        while (requested < size) {
            if (!listener.onPieceDownloaded((100 * downloaded) / size, downloaded)) {
                return null;
            }

            int packetSize = PAGE_SIZE_SLOW;

            Future<TLObject> resFuture = downloadService.submit(new DownloadPartTask(dcId, fileLocation, requested, packetSize));
            requested += packetSize;
            requestedCount++;
            active.add(new Pair<Future<TLObject>, Integer>(resFuture, nextDownloadBlock++));
            int count;
            do {
                count = 0;
                for (Pair<Future<TLObject>, Integer> task : active) {
                    if (!task.first.isDone()) {
                        count++;
                    } else {
                        active.remove(task);
                        final TLFile file;
                        try {
                            file = (TLFile) task.first.get();
                            if (file == null) {
                                return null;
                            }
                        } catch (InterruptedException e) {
                            Logger.t(TAG, e);
                            return null;
                        } catch (ExecutionException e) {
                            Logger.t(TAG, e);
                            return null;
                        }
                        downloadedBlocks.put(task.second, file.getBytes());
                        downloaded += file.getBytes().length;

                        if (!listener.onPieceDownloaded((100 * downloaded) / size, downloaded)) {
                            return null;
                        }
                    }
                }
                if (count >= THREADS_COUNT) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Logger.t(TAG, e);
                        return null;
                    }
                }
            } while (count >= THREADS_COUNT);

            while (nextSaveBlock < requestedCount) {
                if (downloadedBlocks.containsKey(nextSaveBlock)) {
                    try {
                        stream.write(downloadedBlocks.get(nextSaveBlock));
                    } catch (IOException e) {
                        Logger.t(TAG, e);
                        try {
                            stream.close();
                        } catch (IOException e1) {
                            Logger.t(TAG, e1);
                        }
                        return null;
                    }
                    downloadedBlocks.remove(nextSaveBlock);
                    nextSaveBlock++;
                } else {
                    break;
                }
            }
        }

        for (Pair<Future<TLObject>, Integer> task : active) {
            final TLFile file;
            try {
                file = (TLFile) task.first.get();
                if (file == null) {
                    return null;
                }
            } catch (InterruptedException e) {
                Logger.t(TAG, e);
                return null;
            } catch (ExecutionException e) {
                Logger.t(TAG, e);
                return null;
            }
            downloadedBlocks.put(task.second, file.getBytes());
            downloaded += task.second;
            if (!listener.onPieceDownloaded((100 * downloaded) / size, downloaded)) {
                return null;
            }
        }

        while (nextSaveBlock < requestedCount) {
            if (downloadedBlocks.containsKey(nextSaveBlock)) {
                try {
                    stream.write(downloadedBlocks.get(nextSaveBlock++));
                } catch (IOException e) {
                    Logger.t(TAG, e);
                    try {
                        stream.close();
                    } catch (IOException e1) {
                        Logger.t(TAG, e1);
                    }
                    return null;
                }
            }
        }

        try {
            stream.close();
        } catch (IOException e) {
            Logger.t(TAG, e);
            return null;
        }

        if (fileLocation instanceof TLLocalEncryptedFileLocation) {
            String decrypted = getDownloadTempFile();
            try {
                CryptoUtils.AES256IGEDecrypt(new File(destFile), new File(decrypted), ((TLLocalEncryptedFileLocation) fileLocation).getIv(), ((TLLocalEncryptedFileLocation) fileLocation).getKey());
            } catch (IOException e) {
                Logger.t(TAG, e);
                return null;
            }
            destFile = decrypted;
        }

        return destFile;
    }

    private class DownloadPartTask implements Callable<TLObject> {

        private TLAbsLocalFileLocation fileLocation;
        private int offset;
        private int limit;
        private int dcId;

        public DownloadPartTask(int dcId, TLAbsLocalFileLocation fileLocation, int offset, int limit) {
            this.fileLocation = fileLocation;
            this.dcId = dcId;
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public TLObject call() throws Exception {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            final TLAbsInputFileLocation location;
            if (fileLocation instanceof TLLocalFileLocation) {
                location = new TLInputFileLocation(((TLLocalFileLocation) fileLocation).getVolumeId(), ((TLLocalFileLocation) fileLocation).getLocalId(), ((TLLocalFileLocation) fileLocation).getSecret());
            } else if (fileLocation instanceof TLLocalFileVideoLocation) {
                location = new TLInputVideoFileLocation(((TLLocalFileVideoLocation) fileLocation).getVideoId(), ((TLLocalFileVideoLocation) fileLocation).getAccessHash());
            } else if (fileLocation instanceof TLLocalEncryptedFileLocation) {
                location = new TLInputEncryptedFileLocation(((TLLocalEncryptedFileLocation) fileLocation).getId(), ((TLLocalEncryptedFileLocation) fileLocation).getAccessHash());
            } else {
                return null;
            }

            return application.getApi().doGetFile(dcId, location, offset, limit);
        }
    }
}
