package org.telegram.android.kernel;

import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;
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
        techKernel = new TechKernel(application);
    }

    public void initLifeKernel() {
        lifeKernel = new LifeKernel(this);
    }

    public void initBasicUiKernel() {
        uiKernel = new UiKernel(this);
    }

    public void initSearchKernel() {
        searchKernel = new SearchKernel(application);
    }

    public void initAuthKernel() {
        authKernel = new AuthKernel(this);
    }

    public void initSettingsKernel() {
        settingsKernel = new SettingsKernel(this);
    }

    public void initStorageKernel() {
        storageKernel = new StorageKernel(this);
    }

    public void initSourcesKernel() {
        dataSourceKernel = new DataSourceKernel(this);
    }

    public void initFileKernel() {
        fileKernel = new FileKernel(this);
    }

    public void initEncryptedKernel() {
        encryptedKernel = new EncryptedKernel(this);
    }

    public void initSyncKernel() {
        syncKernel = new SyncKernel(this);
    }

    public void initApiKernel() {
        apiKernel = new ApiKernel(this);
    }

    public void runKernels() {
        storageKernel.runKernel();
        apiKernel.runKernel();
        syncKernel.runKernel();
        lifeKernel.runKernel();
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
        authKernel.logOut();
        storageKernel.logOut();
        settingsKernel.logOut();
        apiKernel.updateTelegramApi();
        syncKernel.logOut();
        dataSourceKernel.logOut();
        uiKernel.logOut();
    }
}
