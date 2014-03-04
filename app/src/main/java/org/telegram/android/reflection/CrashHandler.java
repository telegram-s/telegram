package org.telegram.android.reflection;

import com.crashlytics.android.Crashlytics;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 4:02
 */
public class CrashHandler {
    public static void init(TelegramApplication application) {

        Crashlytics.start(application);

        // Flushing logs to disk
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                try {
                    Logger.t("UNHANDLED", ex);
                    Logger.dropOnCrash();
                } catch (Throwable t) {

                }
                originalHandler.uncaughtException(thread, ex);
            }
        });
    }

    public static void logHandledException(Throwable e) {
        Crashlytics.logException(e);
    }

    public static void setUid(int uid, int dcId, String key) {
        Crashlytics.setInt("dc_id", dcId);
        Crashlytics.setString("auth_key_id", key);
        Crashlytics.setUserIdentifier("" + uid);
    }

    public static void removeUid() {
        Crashlytics.setInt("dc_id", 0);
        Crashlytics.setString("auth_key_id", "");
        Crashlytics.setUserIdentifier("" + 0);
    }
}
