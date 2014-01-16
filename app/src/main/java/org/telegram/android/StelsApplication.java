package org.telegram.android;

import android.app.Application;
import android.os.SystemClock;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.ImageSupport;
import org.telegram.android.config.NotificationSettings;
import org.telegram.android.core.engines.ModelEngine;
import org.telegram.android.config.WallpaperHolder;
import org.telegram.android.core.*;
import org.telegram.android.core.background.*;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.config.UserSettings;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.*;
import org.telegram.android.log.Logger;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.android.ui.*;
import org.telegram.api.engine.TelegramApi;

/**
 * Author: Korshakov Stepan
 * Created: 22.07.13 0:55
 */
public class StelsApplication extends Application implements ImageSupport {

    private static final String TAG = "StelsApplication";

    private ApplicationKernel kernel;

    private long appCreateTime;
    private long appStartTime;

    private boolean isLoaded = false;

    @Override
    public void onCreate() {
        long start = SystemClock.uptimeMillis();
        appStartTime = start;
        CrashHandler.init(this);
        kernel = new ApplicationKernel(this);
        super.onCreate();

        long initStart = SystemClock.uptimeMillis();

        // None of this objects starts doing something in background until someone ack them about this
        kernel.initTechKernel(); // Technical information about environment. Might be loaded first.
        kernel.initLifeKernel(); // Keeping application alive
        kernel.initBasicUiKernel(); // UI state objects, eg opened page, app state

        kernel.initAuthKernel(); // Authentication kernel. Might be loaded before other kernels.
        kernel.initStorageKernel(); // Database kernel
        kernel.initSourcesKernel(); // UI Data Sources kernel

        kernel.initSettingsKernel(); // User app settings
        kernel.initFileKernel(); // Uploading/Downloading files
        kernel.initSearchKernel(); // Searching in app
        kernel.initEncryptedKernel(); // Encrypted chats kernel

        kernel.initSyncKernel(); // Background sync kernel

        kernel.initApiKernel(); // Initializing api kernel

        Logger.d(TAG, "Kernels created in " + (SystemClock.uptimeMillis() - initStart) + " ms");

        kernel.runKernels();

        kernel.getUiKernel().onAppPause();

        Logger.d(TAG, "Kernels loaded in " + (SystemClock.uptimeMillis() - start) + " ms");

        appCreateTime = SystemClock.uptimeMillis();
    }

    public void onLoaded() {
        if (isLoaded) {
            return;
        }

        Logger.d(TAG, "Kernel: all loading in " + (SystemClock.uptimeMillis() - appStartTime) + " ms");
        Logger.d(TAG, "Kernel: complete loading in " + (SystemClock.uptimeMillis() - appCreateTime) + " ms");
    }

    public boolean isRTL() {
        return kernel.getTechKernel().getTechReflection().isRtl();
    }

    public boolean isSlow() {
        return kernel.getTechKernel().getTechReflection().isSlow();
    }

    public boolean isLoggedIn() {
        return kernel.getAuthKernel().getApiStorage().isAuthenticated();
    }

    public int getCurrentUid() {
        return kernel.getAuthKernel().getApiStorage().getObj().getUid();
    }

//    public void dropLogin() {
//        if (!isLoggedIn())
//            return;
//
//        kernel.getAuthKernel().getApiStorage().resetAuth();
//
//        clearLoginState();
//
//        updateApi();
//
//        if (kernel.getUiKernel().isAppVisible()) {
//            Intent broadcastIntent = new Intent();
//            broadcastIntent.setAction("org.telegram.android.ACTION_LOGOUT");
//            sendBroadcast(broadcastIntent);
//            // startActivity(new Intent().setClass(this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//        } else {
//            NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
//            notification.setTicker("Required login in Telegram");
//            notification.setContentTitle("Telegram");
//            notification.setContentText("Please login again to Telegram");
//            notification.setContentIntent(PendingIntent.getActivity(this, 0, new Intent().setClass(this, StartActivity.class), 0));
//            notification.setSmallIcon(R.drawable.app_notify);
//            notification.setAutoCancel(true);
//            Notification not = notification.build();
//            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            manager.notify(1, not);
//        }
//    }

    public UploadController getUploadController() {
        return kernel.getFileKernel().getUploadController();
    }

    public UserSource getUserSource() {
        return kernel.getDataSourceKernel().getUserSource();
    }

    public TypingStates getTypingStates() {
        return kernel.getSyncKernel().getTypingStates();
    }

    public MessageSender getMessageSender() {
        return kernel.getSyncKernel().getMessageSender();
    }

    public MediaSender getMediaSender() {
        return kernel.getSyncKernel().getMediaSender();
    }


    public DialogSource getDialogSource() {
        return kernel.getDataSourceKernel().getDialogSource();
    }

    public ContactsSource getContactsSource() {
        return kernel.getDataSourceKernel().getContactsSource();
    }

    public EmojiProcessor getEmojiProcessor() {
        return getUiKernel().getEmojiProcessor();
    }

    public Notifications getNotifications() {
        return getUiKernel().getNotifications();
    }

    public NotificationSettings getNotificationSettings() {
        return kernel.getSettingsKernel().getNotificationSettings();
    }

    public void notifyUIUpdate() {
        getDataSourceKernel().notifyUIUpdate();
    }

    public UpdateProcessor getUpdateProcessor() {
        return kernel.getSyncKernel().getUpdateProcessor();
    }


    public ModelEngine getEngine() {
        return kernel.getStorageKernel().getModel();
    }

    public ApiStorage getApiStorage() {
        return kernel.getAuthKernel().getApiStorage();
    }

    public TelegramApi getApi() {
        return kernel.getApiKernel().getApi();
    }

    public ChatSource getChatSource() {
        return kernel.getDataSourceKernel().getChatSource();
    }

    public DownloadManager getDownloadManager() {
        return kernel.getFileKernel().getDownloadManager();
    }

    public UiResponsibility getResponsibility() {
        return kernel.getUiKernel().getResponsibility();
    }

    public UserSettings getUserSettings() {
        return kernel.getSettingsKernel().getUserSettings();
    }

    public WallpaperHolder getWallpaperHolder() {
        return kernel.getUiKernel().getWallpaperHolder();
    }

    public ConnectionMonitor getMonitor() {
        return kernel.getTechKernel().getMonitor();
    }

    public EncryptionController getEncryptionController() {
        return kernel.getEncryptedKernel().getEncryptionController();
    }

    public EncryptedChatSource getEncryptedChatSource() {
        return kernel.getDataSourceKernel().getEncryptedChatSource();
    }

    public TextSaver getTextSaver() {
        return getUiKernel().getTextSaver();
    }

    public SelfDestructProcessor getSelfDestructProcessor() {
        return kernel.getEncryptedKernel().getSelfDestructProcessor();
    }

    public VersionHolder getVersionHolder() {
        return kernel.getTechKernel().getVersionHolder();
    }

    public ApplicationKernel getKernel() {
        return kernel;
    }

    public UiKernel getUiKernel() {
        return kernel.getUiKernel();
    }

    public TechKernel getTechKernel() {
        return kernel.getTechKernel();
    }

    public SearchKernel getSearchKernel() {
        return kernel.getSearchKernel();
    }

    public DataSourceKernel getDataSourceKernel() {
        return kernel.getDataSourceKernel();
    }

    public SettingsKernel getSettingsKernel() {
        return kernel.getSettingsKernel();
    }

    public SyncKernel getSyncKernel() {
        return kernel.getSyncKernel();
    }


    @Override
    public ImageController getImageController() {
        return kernel.getUiKernel().getImageController();
    }
}