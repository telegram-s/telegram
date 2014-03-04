package org.telegram.android.kernel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import org.telegram.android.R;
import org.telegram.android.core.background.UpdateProcessor;
import org.telegram.android.log.Logger;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.android.util.NativeAES;
import org.telegram.android.util.NativePQ;
import org.telegram.api.TLAbsUpdates;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.mtproto.secure.pq.PQSolver;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class ApiKernel {
    private static final String TAG = "ApiKernel";
    private ApplicationKernel kernel;

    private TelegramApi api;

    public ApiKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
    }

    public ApplicationKernel getKernel() {
        return kernel;
    }

    public void runKernel() {
        // CryptoUtils.setAESImplementation(new NativeAES());
        try {
            PQSolver.setCurrentImplementation(new NativePQ());
        } catch (Exception e) {
            CrashHandler.logHandledException(e);
        }

        api = new TelegramApi(kernel.getAuthKernel().getApiStorage(), new AppInfo(5, Build.MODEL, Build.VERSION.RELEASE, kernel.getTechKernel().getTechReflection().getAppVersion(),
                kernel.getApplication().getString(R.string.st_lang)), new ApiCallback() {
            @Override
            public void onAuthCancelled(TelegramApi api) {
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
                UpdateProcessor processor = kernel.getSyncKernel().getUpdateProcessor();
                if (processor != null) {
                    processor.invalidateUpdates();
                }
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

        BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Logger.w(TAG, "Network Type Changed");
                api.resetNetworkBackoff();
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        kernel.getApplication().registerReceiver(networkStateReceiver, filter);
    }

    public TelegramApi getApi() {
        return api;
    }
}
