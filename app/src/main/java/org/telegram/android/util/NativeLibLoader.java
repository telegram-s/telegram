package org.telegram.android.util;

import org.telegram.android.reflection.CrashHandler;

/**
 * Created by ex3ndr on 04.03.14.
 */
public class NativeLibLoader {

    public static synchronized void loadLib() {
        try {
            System.loadLibrary("timg");
        } catch (Throwable t) {
            try {
                System.load("/data/data/org.telegram.android/lib/libtimg.so");
            } catch (Throwable t2) {
                try {
                    System.load("/data/data/org.telegram.android.beta/lib/libtimg.so");
                } catch (Throwable t3) {
                    CrashHandler.logHandledException(t);
                }
            }
        }

    }
}
