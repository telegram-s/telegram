package org.telegram.android.kernel;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.telegram.android.StelsApplication;
import org.telegram.android.config.DebugSettings;
import org.telegram.android.config.SystemConfig;
import org.telegram.android.core.ConnectionMonitor;
import org.telegram.android.core.VersionHolder;
import org.telegram.android.log.Logger;
import org.telegram.android.reflection.TechReflection;

/**
 * Created by ex3ndr on 16.11.13.
 */
public class TechKernel {

    private static final String TAG = "TechKernel";

    private StelsApplication application;

    private TechReflection techReflection;
    private DebugSettings debugSettings;
    private VersionHolder versionHolder;
    private ConnectionMonitor monitor;
    private SystemConfig systemConfig;

    public TechKernel(StelsApplication application) {
        this.application = application;

        long start = System.currentTimeMillis();
        techReflection = new TechReflection(application);
        Logger.d(TAG, "TechReflection in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        monitor = new ConnectionMonitor(application);
        Logger.d(TAG, "ConnectionMonitor in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        debugSettings = new DebugSettings(application);
        if (debugSettings.isSaveLogs()) {
            Logger.enableDiskLog();
        } else {
            Logger.disableDiskLog();
        }
        Logger.d(TAG, "DebugSettings in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();

        versionHolder = new VersionHolder(application);
        versionHolder.tryLoad();

        try {
            PackageInfo pInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
            int versionCode = pInfo.versionCode;
            if (!versionHolder.isWasUpgraded()) {
                if (versionHolder.getCurrentVersionInstalled() == 0) {
//                  if (isLoggedIn()) {
                    versionHolder.setWasUpgraded(false);
//                  }
                } else {
                    if (versionHolder.getCurrentVersionInstalled() != versionCode) {
                        versionHolder.setWasUpgraded(true);
                    }
                }
            }
            versionHolder.saveCurrentVersion(versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Logger.d(TAG, "VersionHolder in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();

        this.systemConfig = new SystemConfig(application);
        Logger.d(TAG, "SystemConfig in " + (System.currentTimeMillis() - start) + " ms");
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    public TechReflection getTechReflection() {
        return techReflection;
    }

    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    public VersionHolder getVersionHolder() {
        return versionHolder;
    }

    public ConnectionMonitor getMonitor() {
        return monitor;
    }
}
