package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import com.extradea.framework.persistence.ContextPersistence;
import org.telegram.android.kernel.compat.CompatObjectInputStream;
import org.telegram.android.kernel.compat.Compats;
import org.telegram.android.kernel.compat.v6.CompatVersionHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 31.10.13
 * Time: 19:09
 */
public class VersionHolder {

    private SharedPreferences preferences;

    protected int prevVersionInstalled;
    protected int currentVersionInstalled;
    protected boolean wasUpgraded;

    public VersionHolder(Context context) {
        preferences = context.getSharedPreferences("org.telegram.android.VersionHolder.pref", Context.MODE_PRIVATE);
        prevVersionInstalled = preferences.getInt("prevVersionInstalled", 0);
        currentVersionInstalled = preferences.getInt("currentVersionInstalled", 0);
        wasUpgraded = preferences.getBoolean("wasUpgraded", false);

        File obsoleteFile = new File(context.getFilesDir().getAbsolutePath(), "org.telegram.android.core.VersionHolder.sav");
        if (obsoleteFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(obsoleteFile);
                ObjectInputStream stream = new CompatObjectInputStream(inputStream, Compats.VERSION_HOLDER);
                CompatVersionHolder versionHolder = (CompatVersionHolder) stream.readObject();
                prevVersionInstalled = versionHolder.getPrevVersionInstalled();
                currentVersionInstalled = versionHolder.getCurrentVersionInstalled();
                wasUpgraded = versionHolder.isWasUpgraded();
                preferences.edit().putInt("prevVersionInstalled", prevVersionInstalled)
                        .putInt("currentVersionInstalled", currentVersionInstalled)
                        .putBoolean("wasUpgraded", wasUpgraded).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            obsoleteFile.delete();
        }
    }

    public int getPrevVersionInstalled() {
        return prevVersionInstalled;
    }

    public int getCurrentVersionInstalled() {
        return currentVersionInstalled;
    }

    public boolean isWasUpgraded() {
        return wasUpgraded;
    }

    public void setWasUpgraded(boolean wasUpgraded) {
        this.wasUpgraded = wasUpgraded;
        this.prevVersionInstalled = currentVersionInstalled;
    }

    public void markFinishedUpgrade() {
        this.wasUpgraded = false;
        this.prevVersionInstalled = 0;
        preferences.edit().putBoolean("wasUpgraded", wasUpgraded)
                .putInt("prevVersionInstalled", prevVersionInstalled).commit();
    }

    public void saveCurrentVersion(int versionCode) {
        this.currentVersionInstalled = versionCode;
        preferences.edit()
                .putInt("currentVersionInstalled", currentVersionInstalled).commit();
    }
}
