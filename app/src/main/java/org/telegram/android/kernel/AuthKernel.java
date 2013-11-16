package org.telegram.android.kernel;

import android.accounts.Account;
import android.accounts.AccountManager;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.log.Logger;
import org.telegram.api.TLAbsUser;
import org.telegram.api.auth.TLAuthorization;

import java.io.File;
import java.util.ArrayList;

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

        if (storage.isAuthenticated()) {
            // Get existing or recreate system Account
            AccountManager am = AccountManager.get(kernel.getApplication());
            Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
            if (accounts.length == 0) {
                Account account = new Account(storage.getObj().getPhone(), ACCOUNT_TYPE);
                am.addAccountExplicitly(account, "", null);
                this.account = account;
            } else {
                account = accounts[0];
            }
        } else {
            this.account = null;
            AccountManager am = AccountManager.get(kernel.getApplication());
            Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
            for (Account c : accounts) {
                am.removeAccount(c, null, null);
            }
        }
    }

    public void logIn(TLAuthorization authorization) {
        storage.doAuth(authorization);
        ArrayList<TLAbsUser> users = new ArrayList<TLAbsUser>();
        users.add(authorization.getUser());
        kernel.getStorageKernel().getModel().onUsers(users);
        checkState();
    }

    public void logOut() {
        storage.resetAuth();
        checkState();
    }

    public boolean isLoggedIn() {
        return storage.isAuthenticated();
    }
}
