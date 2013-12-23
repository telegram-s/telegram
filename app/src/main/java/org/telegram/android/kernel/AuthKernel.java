package org.telegram.android.kernel;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.SystemClock;
import org.telegram.android.R;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.compat.CompatObjectInputStream;
import org.telegram.android.kernel.compat.Compats;
import org.telegram.android.kernel.compat.state.CompatDc;
import org.telegram.android.kernel.compat.state.CompatDcConfig;
import org.telegram.android.kernel.compat.state.CompatDcKey;
import org.telegram.android.kernel.compat.state.CompatSessionKey;
import org.telegram.android.kernel.compat.v1.CompatCredentials;
import org.telegram.android.kernel.compat.v1.TLUserSelfCompat;
import org.telegram.android.kernel.compat.v2.CompatCredentials2;
import org.telegram.android.kernel.compat.v2.TLUserSelfCompat2;
import org.telegram.android.kernel.compat.v3.CompatAuthCredentials3;
import org.telegram.android.log.Logger;
import org.telegram.android.reflection.CrashHandler;
import org.telegram.api.TLAbsUser;
import org.telegram.api.auth.TLAuthorization;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class AuthKernel {

    private static final String TAG = "AuthKernel";

    private final String ACCOUNT_TYPE;

    private Account account;

    private ApplicationKernel kernel;
    private ApiStorage storage;

    public AuthKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        ACCOUNT_TYPE = kernel.getApplication().getString(R.string.config_account_type);

        long start = SystemClock.uptimeMillis();
        boolean loaded = tryLoadGeneral();
        Logger.d(TAG, "General loading in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();
        if (!loaded) {
            tryLoadObsolete();
        }
        Logger.d(TAG, "Obsolete loading in " + (SystemClock.uptimeMillis() - start) + " ms");


        start = SystemClock.uptimeMillis();
        if (storage == null) {
            storage = new ApiStorage(kernel.getApplication());
        }
        Logger.d(TAG, "ApiStorage loading in " + (SystemClock.uptimeMillis() - start) + " ms");

        start = SystemClock.uptimeMillis();
        checkState();
        Logger.d(TAG, "Api state check loading in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    public ApiStorage getApiStorage() {
        return storage;
    }

    public Account getAccount() {
        return account;
    }

    private boolean tryLoadGeneral() {
        boolean res = new File(kernel.getApplication().getFilesDir().getPath() + "/" + ApiStorage.STORAGE_FILE_NAME).exists();
        if (!res) {
            Logger.d(TAG, "No exisiting configuration");
            return false;
        }
        storage = new ApiStorage(kernel.getApplication());
        return true;
    }

    private void tryLoadObsolete() {
        Logger.d(TAG, "Trying to load obsolete configuration");

        File obsoleteDc = new File(kernel.getApplication().getFilesDir().getPath() + "/org.telegram.android.engine.messaging.centers.DataCenters.sav");
        File obsoleteKeys = new File(kernel.getApplication().getFilesDir().getPath() + "/keys.bin");
        File obsoleteMainSession = new File(kernel.getApplication().getFilesDir().getPath() + "/sessions.bin");
        File obsoleteAuth = new File(kernel.getApplication().getFilesDir().getPath() + "/" + "org.telegram.android.auth.AuthCredentials.sav");

        if (obsoleteDc.exists()) {
            Logger.d(TAG, "Obsolete DC information file exists");
            try {
                CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteDc), Compats.KEYS_V1);
                CompatDc o = (CompatDc) inputStream.readObject();
                Logger.d(TAG, "Loaded obsolete DC information");
                for (Integer key : o.getConfiguration().keySet()) {
                    CompatDcConfig compatConfig = o.getConfiguration().get(key).get(0);
                    Logger.d(TAG, "Address: " + key + " " + compatConfig.getIpAddress() + ":" + compatConfig.getPort());
                    storage.updateDCInfo(key, compatConfig.getIpAddress(), compatConfig.getPort());
                }
            } catch (Exception e) {
                Logger.d(TAG, "Obsolete DC load error.");
                Logger.t(TAG, e);
                return;
            }
        } else {
            Logger.d(TAG, "Obsolete DC not found.");
            return;
        }


        if (obsoleteKeys.exists()) {
            Logger.d(TAG, "Obsolete key information file exists");
            try {
                CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteKeys), Compats.KEYS_V1);
                HashMap<Integer, CompatDcKey> result = (HashMap<Integer, CompatDcKey>) inputStream.readObject();
                Logger.d(TAG, "Loaded obsolete key information");
                for (Integer key : result.keySet()) {
                    CompatDcKey dcKey = result.get(key);
                    storage.putAuthKey(key, dcKey.getAuthKey());
                    storage.setAuthenticated(key, dcKey.isAuthorized());

                    Logger.d(TAG, "Key: " + key + " " + dcKey.isAuthorized());
                }
            } catch (Exception e) {
                Logger.d(TAG, "Obsolete key load error.");
                Logger.t(TAG, e);
                return;
            }
        } else {
            Logger.d(TAG, "Obsolete key not found.");
            return;
        }

        if (obsoleteMainSession.exists()) {
            Logger.d(TAG, "Obsolete main session file exists");
            try {
                CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteMainSession), Compats.KEYS_V1);
                CompatSessionKey o = (CompatSessionKey) inputStream.readObject();
                Logger.d(TAG, "Obsolete main session loaded. DcId: " + o.getDcId());
                storage.setPrimaryDc(o.getDcId());
            } catch (Exception e) {
                Logger.t(TAG, e);
                storage.resetAuth();
                return;
            }
        } else {
            Logger.d(TAG, "Obsolete main session file not found. Resetting auth states.");
            storage.resetAuth();
            return;
        }

        if (obsoleteAuth.exists()) {
            Logger.d(TAG, "Obsolete auth file exists");

            boolean isAuthLoaded = false;

            // Try V3
            try {
                Logger.d(TAG, "Trying to load v3 auth");
                CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteAuth), Compats.VER3);
                CompatAuthCredentials3 authCredentials3 = (CompatAuthCredentials3) inputStream.readObject();
                Logger.d(TAG, "V3 auth loaded");
                storage.doAuth(authCredentials3.getUid(), "UNKNOWN");
                isAuthLoaded = true;
            } catch (Exception e) {
                Logger.d(TAG, "Unable to load V3 auth");
                Logger.t(TAG, e);
            }

            // Try V2
            if (!isAuthLoaded) {
                try {
                    Logger.d(TAG, "Trying to load v2 auth");
                    CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteAuth), Compats.VER2);
                    CompatCredentials2 authCredentials3 = (CompatCredentials2) inputStream.readObject();
                    Logger.d(TAG, "V2 auth loaded");
                    storage.doAuth(((TLUserSelfCompat2) authCredentials3.getUser()).getId(), ((TLUserSelfCompat2) authCredentials3.getUser()).getPhone());
                    isAuthLoaded = true;
                } catch (Exception e) {
                    Logger.d(TAG, "Unable to load V2 auth");
                    Logger.t(TAG, e);
                }
            }

            // Try V1
            if (!isAuthLoaded) {
                try {
                    Logger.d(TAG, "Trying to load v1 auth");
                    CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream(obsoleteAuth), Compats.VER1);
                    CompatCredentials authCredentials3 = (CompatCredentials) inputStream.readObject();
                    Logger.d(TAG, "V1 auth loaded");
                    storage.doAuth(((TLUserSelfCompat) authCredentials3.getUser()).getId(), ((TLUserSelfCompat) authCredentials3.getUser()).getPhone());
                    isAuthLoaded = true;
                } catch (Exception e) {
                    Logger.d(TAG, "Unable to load V1 auth");
                    Logger.t(TAG, e);
                }
            }

            if (!isAuthLoaded) {
                Logger.d(TAG, "Unable to load obsolete auth file. Resetting auth states.");
                storage.resetAuth();
            } else if (!storage.isAuthenticated()) {
                Logger.d(TAG, "Obsolete auth is not logged in. Resetting auth states.");
                storage.resetAuth();
            }

        } else {
            Logger.d(TAG, "Obsolete auth file not found. Resetting auth states.");
            storage.resetAuth();
        }
    }

    private void checkState() {
        // Storage automaticaly checks it's internal state, so just log current state
        Logger.d(TAG, "Current Auth state");
        Logger.d(TAG, "Authenticated: " + storage.isAuthenticated());
        Logger.d(TAG, "Uid: " + storage.getObj().getUid());
        Logger.d(TAG, "Phone: " + storage.getObj().getPhone());
        for (TLKey key : storage.getObj().getKeys()) {
            Logger.d(TAG, "Key: " + key.getDcId() + ":" + key.isAuthorised());
        }
        for (TLDcInfo dc : storage.getObj().getDcInfos()) {
            Logger.d(TAG, "Address: " + dc.getDcId() + " " + dc.getAddress() + ":" + dc.getPort() + " @" + dc.getVersion());
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
        CrashHandler.setUid(storage.getObj().getUid());
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
