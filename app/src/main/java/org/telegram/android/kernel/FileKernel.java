package org.telegram.android.kernel;

import org.telegram.android.core.files.UploadController;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadManager;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class FileKernel {
    private static final String TAG = "FileKernel";
    private ApplicationKernel kernel;

    private UploadController uploadController;
    private DownloadManager downloadManager;

    public FileKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        long start = System.currentTimeMillis();
        uploadController = new UploadController(kernel.getApplication());
        Logger.d(TAG, "UploadController loaded in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        downloadManager = new DownloadManager(kernel.getApplication());
        Logger.d(TAG, "DownloadManager loaded in " + (System.currentTimeMillis() - start) + " ms");
    }

    public UploadController getUploadController() {
        return uploadController;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }
}
