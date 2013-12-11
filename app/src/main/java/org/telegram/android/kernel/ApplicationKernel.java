package org.telegram.android.kernel;

import android.os.HandlerThread;
import android.os.SystemClock;
import com.google.android.gcm.GCMRegistrar;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.LoggerInterface;
import org.telegram.mtproto.log.LogInterface;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class ApplicationKernel {
    private static final String TAG = "Kernel";

    private StelsApplication application;

    private LifeKernel lifeKernel;

    private UiKernel uiKernel;

    private SearchKernel searchKernel;

    private TechKernel techKernel;

    private AuthKernel authKernel;

    private SettingsKernel settingsKernel;

    private StorageKernel storageKernel;

    private DataSourceKernel dataSourceKernel;

    private FileKernel fileKernel;

    private EncryptedKernel encryptedKernel;

    private SyncKernel syncKernel;

    private ApiKernel apiKernel;

    public ApplicationKernel(StelsApplication application) {
        this.application = application;
        initLogging();
        Logger.d(TAG, "--------------- Kernel Created ------------------");
    }

    public StelsApplication getApplication() {
        return application;
    }

    public LifeKernel getLifeKernel() {
        return lifeKernel;
    }

    public UiKernel getUiKernel() {
        return uiKernel;
    }

    public TechKernel getTechKernel() {
        return techKernel;
    }

    public AuthKernel getAuthKernel() {
        return authKernel;
    }

    public SearchKernel getSearchKernel() {
        return searchKernel;
    }

    public SettingsKernel getSettingsKernel() {
        return settingsKernel;
    }

    public StorageKernel getStorageKernel() {
        return storageKernel;
    }

    public DataSourceKernel getDataSourceKernel() {
        return dataSourceKernel;
    }

    public FileKernel getFileKernel() {
        return fileKernel;
    }

    public ApiKernel getApiKernel() {
        return apiKernel;
    }

    public SyncKernel getSyncKernel() {
        return syncKernel;
    }

    public EncryptedKernel getEncryptedKernel() {
        return encryptedKernel;
    }

    private void initLogging() {
        Logger.init(application);
        org.telegram.api.engine.Logger.registerInterface(new LoggerInterface() {
            @Override
            public void w(String tag, String message) {
                Logger.w("API|" + tag, message);
            }

            @Override
            public void d(String tag, String message) {
                Logger.d("API|" + tag, message);
            }

            @Override
            public void e(String tag, Throwable t) {
                Logger.t("API|" + tag, t);
            }
        });
        org.telegram.mtproto.log.Logger.registerInterface(new LogInterface() {
            @Override
            public void w(String tag, String message) {
                Logger.w(tag, message);
            }

            @Override
            public void d(String tag, String message) {
                Logger.d(tag, message);
            }

            @Override
            public void e(String tag, Throwable t) {
                Logger.t(tag, t);
            }
        });
    }

    public void initTechKernel() {
        long start = SystemClock.uptimeMillis();
        techKernel = new TechKernel(application);
        Logger.d(TAG, "TechKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initLifeKernel() {
        long start = SystemClock.uptimeMillis();
        lifeKernel = new LifeKernel(this);
        Logger.d(TAG, "LifeKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initBasicUiKernel() {
        long start = SystemClock.uptimeMillis();
        uiKernel = new UiKernel(this);
        Logger.d(TAG, "UiKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initSearchKernel() {
        long start = SystemClock.uptimeMillis();
        searchKernel = new SearchKernel(application);
        Logger.d(TAG, "SearchKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initAuthKernel() {
        long start = SystemClock.uptimeMillis();
        authKernel = new AuthKernel(this);
        Logger.d(TAG, "AuthKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");

        if (authKernel.isLoggedIn()) {
            CrashHandler.setUid(authKernel.getApiStorage().getObj().getUid());
        }
    }

    public void initSettingsKernel() {
        long start = SystemClock.uptimeMillis();
        settingsKernel = new SettingsKernel(this);
        Logger.d(TAG, "SettingsKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initStorageKernel() {
        long start = SystemClock.uptimeMillis();
        storageKernel = new StorageKernel(this);
        Logger.d(TAG, "StorageKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initSourcesKernel() {
        long start = SystemClock.uptimeMillis();
        dataSourceKernel = new DataSourceKernel(this);
        Logger.d(TAG, "DataSourceKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initFileKernel() {
        long start = SystemClock.uptimeMillis();
        fileKernel = new FileKernel(this);
        Logger.d(TAG, "FileKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initEncryptedKernel() {
        long start = SystemClock.uptimeMillis();
        encryptedKernel = new EncryptedKernel(this);
        Logger.d(TAG, "EncryptedKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initSyncKernel() {
        long start = SystemClock.uptimeMillis();
        syncKernel = new SyncKernel(this);
        Logger.d(TAG, "SyncKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void initApiKernel() {
        long start = SystemClock.uptimeMillis();
        apiKernel = new ApiKernel(this);
        Logger.d(TAG, "ApiKernel init in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void runKernels() {
        long kernelsStart = SystemClock.uptimeMillis();
        long start = SystemClock.uptimeMillis();
        storageKernel.runKernel();
        Logger.d(TAG, "Storate run in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        apiKernel.runKernel();
        Logger.d(TAG, "ApiKernel run in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        syncKernel.runKernel();
        Logger.d(TAG, "SyncKernel run in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        lifeKernel.runKernel();
        Logger.d(TAG, "LifeKernel run in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        dataSourceKernel.runKernel();
        Logger.d(TAG, "DataSourceKernel run in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        GCMRegistrar.register(application, "216315056253");
        Logger.d(TAG, "Push register in " + (SystemClock.uptimeMillis() - start) + " ms");

        Logger.d(TAG, "Kernels started in " + (SystemClock.uptimeMillis() - kernelsStart) + " ms");
    }

    public void logIn(TLAuthorization authorization) {
        storageKernel.logIn();
        authKernel.logIn(authorization);
        settingsKernel.logIn();
        syncKernel.logIn();
        dataSourceKernel.logIn();
        uiKernel.logIn();
    }

    // Executing may take time
    public void logOut() {
        long start = SystemClock.uptimeMillis();
        authKernel.logOut();
        Logger.d(TAG, "LogOut: atuhKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        storageKernel.logOut();
        Logger.d(TAG, "LogOut: storageKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        settingsKernel.logOut();
        Logger.d(TAG, "LogOut: settingsKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        apiKernel.updateTelegramApi();
        Logger.d(TAG, "LogOut: apiKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        syncKernel.logOut();
        Logger.d(TAG, "LogOut: syncKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        dataSourceKernel.logOut();
        Logger.d(TAG, "LogOut: dataSourceKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
        start = SystemClock.uptimeMillis();
        uiKernel.logOut();
        Logger.d(TAG, "LogOut: uiKernel in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public void sendEvent(String type) {
        syncKernel.getBackgroundSync().sendLog(type, "");
    }

    public void sendEvent(String type, String message) {
        syncKernel.getBackgroundSync().sendLog(type, message);
    }
}
