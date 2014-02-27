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

    public static void logHandledException(Exception e) {
        Crashlytics.logException(e);
    }

    public static void setUid(int uid) {
        Crashlytics.setUserIdentifier("" + uid);
    }
}
