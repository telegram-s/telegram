package org.telegram.android.kernel;

import android.os.Build;
import org.telegram.android.R;
import org.telegram.android.core.background.UpdateProcessor;
import org.telegram.android.kernel.api.AuthController;
import org.telegram.api.TLAbsUpdates;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class ApiKernel {
    private ApplicationKernel kernel;

    private TelegramApi api;

    private AuthController authController;

    public ApiKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        this.authController = new AuthController(this);
    }

    public AuthController getAuthController() {
        return authController;
    }

    public ApplicationKernel getKernel() {
        return kernel;
    }

    public void runKernel() {
        updateTelegramApi();
    }

    public void switchToDc(int dcId) {
        kernel.getAuthKernel().getApiStorage().switchToPrimaryDc(dcId);
        updateTelegramApi();
        authController.check();
    }

    public void updateTelegramApi() {
        if (kernel.getAuthKernel().getApiStorage().getAuthKey(kernel.getAuthKernel().getApiStorage().getPrimaryDc()) != null) {
            api = new TelegramApi(kernel.getAuthKernel().getApiStorage(), new AppInfo(5, Build.MODEL, Build.VERSION.RELEASE, kernel.getTechKernel().getTechReflection().getAppVersion(),
                    kernel.getApplication().getString(R.string.st_lang)), new ApiCallback() {
                @Override
                public void onApiDies(TelegramApi api) {
                    if (api != ApiKernel.this.api) {
                        return;
                    }
                    kernel.logOut();
                }

                @Override
                public void onUpdatesInvalidated(TelegramApi api) {
                    if (api != ApiKernel.this.api) {
                        return;
                    }
                    if (!kernel.getAuthKernel().isLoggedIn()) {
                        return;
                    }
                    kernel.getSyncKernel().getUpdateProcessor().invalidateUpdates();
                }

                @Override
                public void onUpdate(TLAbsUpdates updates) {
                    if (api != ApiKernel.this.api) {
                        return;
                    }
                    if (!kernel.getAuthKernel().isLoggedIn()) {
                        return;
                    }
                    UpdateProcessor updateProcessor = kernel.getSyncKernel().getUpdateProcessor();
                    if (updateProcessor != null) {
                        updateProcessor.onMessage(updates);
                    }
                }
            });
        } else {
            api = null;
        }
        authController.check();
    }

    public TelegramApi getApi() {
        return api;
    }
}
