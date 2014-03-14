package org.telegram.android;

import android.app.Application;
import org.telegram.android.config.NotificationSettings;
import org.telegram.android.core.engines.ModelEngine;
import org.telegram.android.config.WallpaperHolder;
import org.telegram.android.core.*;
import org.telegram.android.core.background.*;
import org.telegram.android.core.files.UploadController;
import org.telegram.android.config.UserSettings;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.*;
import org.telegram.android.media.DownloadManager;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.android.ui.*;
import org.telegram.android.util.NativeLibLoader;
import org.telegram.api.engine.TelegramApi;

/**
 * Author: Korshakov Stepan
 * Created: 22.07.13 0:55
 */
public class TelegramApplication extends Application {

    private ApplicationKernel kernel;
    private KernelsLoader kernelsLoader;

    @Override
    public void onCreate() {
        if (kernel != null) {
            super.onCreate();
            return;
        }

        NativeLibLoader.initNativeLibs(this);

        CrashHandler.init(this);
        kernel = new ApplicationKernel(this);
        super.onCreate();

        kernelsLoader = new KernelsLoader();
        kernelsLoader.stagedLoad(kernel);
    }

    public KernelsLoader getKernelsLoader() {
        return kernelsLoader;
    }

    public boolean isRTL() {
        return kernel.getTechKernel().getTechReflection().isRtl();
    }

    public boolean isSlow() {
        return kernel.getTechKernel().getTechReflection().isSlow();
    }

    public boolean isLoggedIn() {
        return kernel.getAuthKernel().isLoggedIn();
    }

    public int getCurrentUid() {
        return kernel.getAuthKernel().getApiStorage().getObj().getUid();
    }

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
}