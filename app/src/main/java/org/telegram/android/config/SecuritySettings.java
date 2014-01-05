package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ex3ndr on 05.01.14.
 */
public class SecuritySettings {

    public static final int LOCK_TYPE_NONE = 0;
    public static final int LOCK_TYPE_DEBUG = 1;

    private SharedPreferences preferences;
    private int lockType = LOCK_TYPE_NONE;
    private int lockTimeout = 60;// Sec

    public SecuritySettings(Context context) {
        preferences = context.getSharedPreferences("org.telegram.android.SecuritySettings.pref", Context.MODE_PRIVATE);
        lockType = preferences.getInt("lockType", LOCK_TYPE_NONE);
        lockTimeout = preferences.getInt("lockTimeout", 60);
    }

    public boolean isLockEnabled() {
        return lockType != LOCK_TYPE_NONE;
    }

    public int getLockType() {
        return lockType;
    }

    public void setLockType(int lockType) {
        this.lockType = lockType;
        preferences.edit().putInt("lockType", lockType).commit();
    }

    public int getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(int lockTimeout) {
        this.lockTimeout = lockTimeout;
        preferences.edit().putInt("lockTimeout", lockTimeout).commit();
    }

    public void clearSettings() {
        String[] keys = preferences.getAll().keySet().toArray(new String[0]);
        SharedPreferences.Editor editor = preferences.edit();
        for (String k : keys) {
            editor.remove(k);
        }
        editor.commit();
        lockTimeout = 60;
        lockType = LOCK_TYPE_NONE;
    }
}
