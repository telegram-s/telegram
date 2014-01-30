package org.telegram.android.kernel;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.telegram.android.log.Logger;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ex3ndr on 20.01.14.
 */
public class KernelsLoader {

    private static final String TAG = "KernelsLoader";

    public static interface KernelsLoadingListener {
        public void onKernelsLoaded();
    }

    private CopyOnWriteArrayList<KernelsLoadingListener> listeners = new CopyOnWriteArrayList<KernelsLoadingListener>();
    private Handler handler = new Handler(Looper.getMainLooper());

    private Thread loaderThread;
    private boolean isLoaded = false;

    public void addListener(KernelsLoadingListener loadingListener) {
        if (listeners.contains(loadingListener)) {
            return;
        }
        listeners.add(loadingListener);
    }

    public void removeListener(KernelsLoadingListener listener) {
        listeners.remove(listener);
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    private void notifyLoaded() {
        Logger.d(TAG, "Loaded");
        isLoaded = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (KernelsLoadingListener loadingListener : listeners) {
                    loadingListener.onKernelsLoaded();
                }
            }
        });
    }

    private void ensureLoaded(ApplicationKernel kernel) {
        if (kernel.getAuthKernel() == null) {
            throw new IllegalStateException("Auth kernel empty");
        }

        if (kernel.getAuthKernel().isLoggedIn()) {
            if (kernel.getDataSourceKernel().getDialogSource() == null) {
                throw new IllegalStateException("Dialogs are not loaded");
            }
        }
    }

    public boolean stagedLoad(final ApplicationKernel kernel) {
        long initStart = SystemClock.uptimeMillis();

        // None of this objects starts doing something in background until someone ack them about this
        // At this stage nothing might request for data from storage kernel
        kernel.initTechKernel(); // Technical information about environment. Might be loaded first.
        kernel.initLifeKernel(); // Keeping application alive
        kernel.initBasicUiKernel(); // UI state objects, eg opened page, app state

        kernel.initAuthKernel(); // Authentication kernel. Might be loaded before other kernels.

        kernel.initSettingsKernel(); // User app settings
        kernel.initFileKernel(); // Uploading/Downloading files
        kernel.initSearchKernel(); // Searching in app
        kernel.initEncryptedKernel(); // Encrypted chats kernel

        kernel.initApiKernel(); // Initializing api kernel

        if (kernel.asyncRequiredInit()) {
            Logger.d(TAG, "Kernels pre created in " + (SystemClock.uptimeMillis() - initStart) + " ms");
            loaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    long upgradeStart = SystemClock.uptimeMillis();
                    kernel.initStorageKernel(); // Database kernel
                    kernel.initSyncKernel(); // Background sync kernel
                    kernel.initSourcesKernel(); // UI Data Sources kernel
                    kernel.runKernels(); // Run all kernels
                    ensureLoaded(kernel);
                    Logger.d(TAG, "Storage kernels updated " + (SystemClock.uptimeMillis() - upgradeStart) + " ms");
                    notifyLoaded();
                }
            });
            loaderThread.start();
            return false;
        }

        kernel.initStorageKernel(); // Database kernel
        kernel.initSyncKernel(); // Background sync kernel
        kernel.initSourcesKernel(); // UI Data Sources kernel

        Logger.d(TAG, "Kernels created in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        kernel.runKernels();

        ensureLoaded(kernel);

        kernel.getUiKernel().onAppPause();

        Logger.d(TAG, "Kernels loaded in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        notifyLoaded();
        return true;
    }

    public void asyncLoadKernels(final ApplicationKernel applicationKernel) {
        loaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                performLoad(applicationKernel);
                notifyLoaded();
            }
        });
        loaderThread.start();
    }

    public void loadKernels(ApplicationKernel applicationKernel) {
        performLoad(applicationKernel);
    }

    private void performLoad(ApplicationKernel kernel) {
        long initStart = SystemClock.uptimeMillis();

        // None of this objects starts doing something in background until someone ack them about this
        kernel.initTechKernel(); // Technical information about environment. Might be loaded first.
        kernel.initLifeKernel(); // Keeping application alive
        kernel.initBasicUiKernel(); // UI state objects, eg opened page, app state

        kernel.initAuthKernel(); // Authentication kernel. Might be loaded before other kernels.

        kernel.initSettingsKernel(); // User app settings
        kernel.initFileKernel(); // Uploading/Downloading files
        kernel.initSearchKernel(); // Searching in app
        kernel.initEncryptedKernel(); // Encrypted chats kernel

        kernel.initSyncKernel(); // Background sync kernel

        kernel.initApiKernel(); // Initializing api kernel

        kernel.initStorageKernel(); // Database kernel
        kernel.initSourcesKernel(); // UI Data Sources kernel

        Logger.d(TAG, "Kernels created in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        kernel.runKernels();

        kernel.getUiKernel().onAppPause();

        Logger.d(TAG, "Kernels loaded in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        isLoaded = true;
    }
}
