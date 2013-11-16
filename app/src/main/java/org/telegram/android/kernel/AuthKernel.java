package org.telegram.android.kernel;

import android.accounts.Account;
import android.accounts.AccountManager;
import org.telegram.android.kernel.api.AuthController;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.log.Logger;

import java.io.File;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class AuthKernel {

    private static final String TAG = "AuthKernel";

    private static final String ACCOUNT_TYPE = "org.telegram.android.account";

    private Account account;

    private ApplicationKernel kernel;
    private ApiStorage storage;

    public AuthKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        if (!tryLoadGeneral()) {
            tryLoadObsolete();
            storage.write();
        }

        if (storage == null) {
            storage = new ApiStorage(kernel.getApplication());
        }
        checkState();
        storage.write();
    }

    public ApiStorage getApiStorage() {
        return storage;
    }

    public Account getAccount() {
        return account;
    }

    private boolean tryLoadGeneral() {
        boolean res = new File(kernel.getApplication().getFilesDir().getPath() + "/" + ApiStorage.STORAGE_FILE_NAME).exists();
        storage = new ApiStorage(kernel.getApplication());
        return res;
    }

    private void tryLoadObsolete() {

    }

    private void checkState() {
        // Storage automaticaly checks it's internal state, so just log current state
        Logger.d(TAG, "Authenticated: " + storage.isAuthenticated());
        Logger.d(TAG, "Uid: " + storage.getObj().getUid());
        Logger.d(TAG, "Phone: " + storage.getObj().getPhone());
        for (TLKey key : storage.getObj().getKeys()) {
            Logger.d(TAG, "Key: " + key.getDcId() + ":" + key.isAuthorised());
        }
        for (TLDcInfo dc : storage.getObj().getDcInfos()) {
            Logger.d(TAG, "Key: " + dc.getDcId() + " " + dc.getAddress() + ":" + dc.getPort());
        }

        // Get existing or recreate system Account
        AccountManager am = AccountManager.get(kernel.getApplication());
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length == 0) {
            Account account = new Account(kernel.getAuthKernel().getApiStorage().getObj().getPhone(), ACCOUNT_TYPE);
            am.addAccountExplicitly(account, "", null);
            this.account = account;
        } else {
            account = accounts[0];
        }
    }

    public void logOut() {
        storage.resetAuth();

        this.account = null;
        AccountManager am = AccountManager.get(kernel.getApplication());
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        for (Account c : accounts) {
            am.removeAccount(c, null, null);
        }

//        // Clearing all messages states
//        for (MessageSource source : messageSources.values()) {
//            source.destroy();
//        }
//        messageSources.clear();
//        MessageSource.clearData(this);
//
//        // Clearing dialogs states
//        dialogSource.destroy();
//        DialogSource.clearData(this);
//
//        // Clearing contacts states
//        contactsSource.destroy();
//        ContactsSource.clearData(this);

//        updateProcessor.destroy();
//        updateProcessor.clearData();
//
//        techSyncer.onLogout();
//
//        getUiKernel().getNotifications().reset();

//        dropData();
    }

    public boolean isLoggedIn() {
        return storage.isAuthenticated();
    }
}
