package org.telegram.android;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.support.v4.app.NotificationCompat;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.ImageSupport;
import org.telegram.android.kernel.api.AuthController;
import org.telegram.android.config.WallpaperHolder;
import org.telegram.android.core.*;
import org.telegram.android.core.background.*;
import org.telegram.android.core.files.DownloadController;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.config.UserSettings;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.*;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.android.screens.ScreenLogicType;
import org.telegram.android.ui.*;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.TelegramApi;

/**
 * Author: Korshakov Stepan
 * Created: 22.07.13 0:55
 */
public class StelsApplication extends Application implements ImageSupport {

    private ApplicationKernel kernel;

    @Override
    public void onCreate() {
        CrashHandler.init(this);
        kernel = new ApplicationKernel(this);
        super.onCreate();
        // None of this objects starts doing something in background until someone ack them about this
        kernel.initTechKernel(); // Technical information about environment. Might be loaded first.
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

        kernel.runKernels();

        kernel.getUiKernel().onAppPause();
    }

    public boolean isRTL() {
        return kernel.getTechKernel().getTechReflection().isRtl();
    }

    public ScreenLogicType getScreenLogicType() {
        return kernel.getUiKernel().getScreenLogicType();
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

    public FastActions getActions() {
        return kernel.getSyncKernel().getActions();
    }

    public TypingStates getTypingStates() {
        return kernel.getSyncKernel().getTypingStates();
    }

    public MessageSender getMessageSender() {
        return kernel.getSyncKernel().getMessageSender();
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

    public AuthController getAuthController() {
        return kernel.getApiKernel().getAuthController();
    }

    public ChatSource getChatSource() {
        return kernel.getDataSourceKernel().getChatSource();
    }

    public DownloadManager getDownloadManager() {
        return kernel.getFileKernel().getDownloadManager();
    }

    public TechSyncer getTechSyncer() {
        return kernel.getSyncKernel().getTechSyncer();
    }

    public DynamicConfig getDynamicConfig() {
        return kernel.getSyncKernel().getDynamicConfig();
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

    public DownloadController getDownloadController() {
        return kernel.getFileKernel().getDownloadController();
    }

    public TextSaver getTextSaver() {
        return getUiKernel().getTextSaver();
    }

    public SelfDestructProcessor getSelfDestructProcessor() {
        return kernel.getEncryptedKernel().getSelfDestructProcessor();
    }

    public EncryptedChatProcessor getEncryptedChatProcessor() {
        return kernel.getEncryptedKernel().getEncryptedChatProcessor();
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

    @Override
    public ImageController getImageController() {
        return kernel.getUiKernel().getImageController();
    }
}