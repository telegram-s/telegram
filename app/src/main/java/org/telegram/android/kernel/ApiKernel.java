package org.telegram.android.kernel;

import android.os.Build;
import org.telegram.android.R;
import org.telegram.android.core.background.UpdateProcessor;
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

    public ApiKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
    }

    public ApplicationKernel getKernel() {
        return kernel;
    }

    public void runKernel() {
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
    }

    public void switchToDc(int dcId) {
        kernel.getAuthKernel().getApiStorage().switchToPrimaryDc(dcId);
        api.switchToDc(dcId);
    }

    public TelegramApi getApi() {
        return api;
    }
}
