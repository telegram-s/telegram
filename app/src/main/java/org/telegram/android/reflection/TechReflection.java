package org.telegram.android.reflection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import org.telegram.android.R;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 3:43
 */
public class TechReflection {

    private static final String SHARED_APP_VERSION = "org.telegram.android.App";

    private boolean isSlow;
    private boolean isRtl;
    private boolean isOnSdCard;
    private String version;
    private boolean isAppUpgraded = false;
    private boolean isDebug = false;
    private int screenSize;

    public boolean isSlow() {
        return isSlow;
    }

    public boolean isRtl() {
        return isRtl;
    }

    public boolean isOnSdCard() {
        return isOnSdCard;
    }

    public String getAppVersion() {
        return version;
    }

    public boolean isAppUpgraded() {
        return isAppUpgraded;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public int getScreenSize() {
        return screenSize;
    }

    public TechReflection(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            isSlow = true;
        } else {
            boolean xlarge = ((context.getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
            boolean large = ((context.getResources().getConfiguration().screenLayout & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) == android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE);
            isSlow = xlarge || large;
        }

        isRtl = context.getResources().getString(R.string.st_lang).equals("ar");
        if (Build.VERSION.SDK_INT >= 17) {
            int direction = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
            if (direction == View.LAYOUT_DIRECTION_RTL) {
                isRtl = true;
            }
        }

        isOnSdCard = isInstalledOnSdCard(context);

        version = getAppVersion(context);

        SharedPreferences preferences = context.getSharedPreferences(SHARED_APP_VERSION, Context.MODE_PRIVATE);
        String oldVer = preferences.getString("app_ver", "");
        if (oldVer.equals(getAppVersion())) {
            isAppUpgraded = false;
        } else {
            preferences.edit().putString("app_ver", getAppVersion()).commit();
            isAppUpgraded = true;
        }

        isDebug = "debug".equals(version);

        screenSize = Math.min(context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels);
    }

    /**
     * Checks if the application is installed on the SD card.
     *
     * @return <code>true</code> if the application is installed on the sd card
     */
    private boolean isInstalledOnSdCard(Context context) {
        // check for API level 8 and higher
        if (Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
                ApplicationInfo ai = pi.applicationInfo;
                return (ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE;
            } catch (PackageManager.NameNotFoundException e) {
                // ignore
            }
        }

        // check for API level 7 - check files dir
        try {
            String filesDir = context.getFilesDir().getAbsolutePath();
            if (filesDir.startsWith("/data/")) {
                return false;
            } else if (filesDir.contains("/mnt/") || filesDir.contains("/sdcard/")) {
                return true;
            }
        } catch (Throwable e) {
            // ignore
        }

        return false;
    }

    private int getNumCores() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Default to return 1 core
            return 1;
        }
    }

    private String getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
}
