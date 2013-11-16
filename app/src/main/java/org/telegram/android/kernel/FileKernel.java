package org.telegram.android.kernel;

import org.telegram.android.core.files.DownloadController;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.media.DownloadManager;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class FileKernel {
    private ApplicationKernel kernel;

    private UploadController uploadController;
    private DownloadController downloadController;
    private DownloadManager downloadManager;

    public FileKernel(ApplicationKernel kernel) {
        this.kernel = kernel;

        uploadController = new UploadController(kernel.getApplication());
        downloadController = new DownloadController(kernel.getApplication());
        downloadManager = new DownloadManager(kernel.getApplication());
    }

    public UploadController getUploadController() {
        return uploadController;
    }

    public DownloadController getDownloadController() {
        return downloadController;
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }
}
