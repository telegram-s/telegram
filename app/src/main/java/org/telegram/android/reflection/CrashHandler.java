package org.telegram.android.reflection;

import com.bugsense.trace.BugSenseHandler;
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
        BugSenseHandler.initAndStartSession(application, "e6d19090");

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
        BugSenseHandler.sendException(e);
    }

    public static void setUid(int uid) {
        BugSenseHandler.setUserIdentifier("u" + uid);
    }
}
