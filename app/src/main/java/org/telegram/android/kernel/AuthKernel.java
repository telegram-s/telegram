package org.telegram.android.kernel;

import android.accounts.Account;
import android.accounts.AccountManager;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.critical.ApiStorage;
import org.telegram.android.kernel.compat.CompatObjectInputStream;
import org.telegram.android.kernel.compat.CompatContextPersistence;
import org.telegram.android.kernel.compat.CompatPersistence;
import org.telegram.android.kernel.compat.Compats;
import org.telegram.android.kernel.compat.state.CompatDc;
import org.telegram.android.kernel.compat.state.CompatDcConfig;
import org.telegram.android.kernel.compat.state.CompatDcKey;
import org.telegram.android.kernel.compat.state.CompatSessionKey;
import org.telegram.android.log.Logger;
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

    private static final String ACCOUNT_TYPE = "org.telegram.android.account";

    private Account account;

    private ApplicationKernel kernel;
    private ApiStorage storage;

    public AuthKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
        if (!tryLoadGeneral()) {
            tryLoadObsolete();
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
        try {
            CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream("/sdcard/obsolete_keys.bin"), Compats.KEYS_V1);
            HashMap<Integer, CompatDcKey> result = (HashMap<Integer, CompatDcKey>) inputStream.readObject();
            for (Integer key : result.keySet()) {
                CompatDcKey dcKey = result.get(key);
                storage.putAuthKey(key, dcKey.getAuthKey());
                storage.setAuthenticated(key, dcKey.isAuthorized());
            }

            // storage.putAuthKey();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream("/sdcard/obsolete_dc.bin"), Compats.KEYS_V1);
            CompatDc o = (CompatDc) inputStream.readObject();

            for (Integer key : o.getConfiguration().keySet()) {
                CompatDcConfig compatConfig = o.getConfiguration().get(key).get(0);
                storage.updateDCInfo(key, compatConfig.getIpAddress(), compatConfig.getPort());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        boolean isSessionLoaded = false;
        try {
            CompatObjectInputStream inputStream = new CompatObjectInputStream(new FileInputStream("/sdcard/obsolete_session.bin"), Compats.KEYS_V1);
            CompatSessionKey o = (CompatSessionKey) inputStream.readObject();
            storage.switchToPrimaryDc(o.getDcId());
            isSessionLoaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (!isSessionLoaded) {
//            storage.resetAuth();
//        }

        File dcStorage = new File(kernel.getApplication().getFilesDir().getPath() + "/" + "org.telegram.android.auth.AuthCredentials.bin");
        if (dcStorage.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(dcStorage);
                byte[] data = new byte[(int) dcStorage.length()];
                inputStream.read(data);
                inputStream.close();
                FileOutputStream outputStream = new FileOutputStream("/sdcard/obsolete_auth.bin");
                outputStream.write(data);
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
