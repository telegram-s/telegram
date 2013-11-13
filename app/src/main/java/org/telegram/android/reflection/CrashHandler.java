package org.telegram.android.reflection;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 4:02
 */
public class CrashHandler {
    public static void init(StelsApplication application) {
        CrittercismConfig config = new CrittercismConfig();
        config.setLogcatReportingEnabled(true);
        config.setDelaySendingAppLoad(true);
        Crittercism.initialize(application, "522b33d9d0d8f70727000008");

        // Flushing logs to disk
        final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Logger.t("UNHANDLED", ex);
                Logger.dropOnCrash();
                originalHandler.uncaughtException(thread, ex);
            }
        });
    }
}
