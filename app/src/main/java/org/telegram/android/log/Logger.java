package org.telegram.android.log;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 01.10.13
 * Time: 18:29
 */
public class Logger {
    private static class LogRecord {
        private long time;
        private String tag;
        private String thread;
        private String message;

        private LogRecord(long time, String tag, String thread, String message) {
            this.time = time;
            this.tag = tag;
            this.thread = thread;
            this.message = message;
        }
    }

    private static final String TAG = "Logger";

    private static final boolean ENABLED = true;
    private static final boolean LOG_THREAD = true;

    private static ArrayList<LogRecord> cachedRecords = new ArrayList<LogRecord>();

    private static String logPath;

    private static boolean isInitied = false;
    private static boolean isEnabled = false;

    private static boolean enqueued = false;
    private static final Object locker = new Object();
    private static BufferedWriter writer;
    private static Thread writerThread;
    private static Handler writerHandler;
    private static final Runnable dropCacheRunnable = new Runnable() {
        @Override
        public void run() {
            dropCache();
        }
    };

    public static void enableDiskLog() {
        isEnabled = true;
    }

    public static void disableDiskLog() {
        isEnabled = false;
    }

    public static synchronized void init(Context context) {
        logPath = context.getFilesDir().getAbsolutePath() + "/log.txt";
        isInitied = true;
        try {
            writer = new BufferedWriter(new FileWriter(logPath, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                writerHandler = new Handler();
                Looper.loop();
            }
        });
        writerThread.start();
    }

    public static synchronized String exportLogs() {
        String destFile = "/sdcard/log_" + System.currentTimeMillis() + ".log";
        dropCache();
        synchronized (locker) {
            try {
                writer.flush();
                writer.close();
                writer = null;
                copy(new File(logPath), new File(destFile));
                writer = new BufferedWriter(new FileWriter(logPath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return destFile;
    }

    public static synchronized void clearLogs() {
        dropCache();
        synchronized (locker) {
            try {
                writer.flush();
                writer.close();
                writer = null;
                new File(logPath).delete();
                writer = new BufferedWriter(new FileWriter(logPath, true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private static synchronized void dropCache() {
        if (ENABLED && isInitied && writer != null) {
            synchronized (locker) {
                LogRecord[] records = null;
                synchronized (cachedRecords) {
                    if (cachedRecords.size() > 0) {
                        records = cachedRecords.toArray(new LogRecord[cachedRecords.size()]);
                        cachedRecords.clear();
                        enqueued = false;
                    }
                }
                if (records != null) {
                    for (LogRecord record : records) {
                        try {
                            writer.write(record.time + "|" + record.tag + "|" + record.thread + "|" + record.message);
                            writer.newLine();
                        } catch (IOException e) {
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                    try {
                        writer.flush();
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }
    }

    private static synchronized void killWriter() {
        if (ENABLED && isInitied) {
            synchronized (locker) {
                if (writerThread != null) {
                    writerThread.interrupt();
                    try {
                        writerThread.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writerThread = null;
                }
            }
        }
    }

    private static synchronized void closeWriter() {
        if (ENABLED && isInitied) {
            synchronized (locker) {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                        writer = null;
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }
    }

    public static synchronized void dropOnCrash() {
        killWriter();
        dropCache();
        closeWriter();
    }

    private static void addLogRecord(LogRecord record) {
        if (isEnabled && isInitied) {
            synchronized (cachedRecords) {
                cachedRecords.add(record);
                if (!enqueued && writerHandler != null) {
                    enqueued = true;
                    writerHandler.postDelayed(dropCacheRunnable, 1000);
                }
            }
        }
    }

    public static void d(String TAG, String message) {
        if (!ENABLED) {
            return;
        }
        if (LOG_THREAD) {
            Log.d("T|" + TAG, Thread.currentThread().getName() + "| " + message);
        } else {
            Log.d("T|" + TAG, message);
        }
        if (isEnabled && isInitied) {
            addLogRecord(new LogRecord(System.currentTimeMillis(), TAG, Thread.currentThread().getName(), message));
        }
    }

    public static void e(String TAG, String message, Exception e) {
        if (!ENABLED) {
            return;
        }
        Log.e("T|" + TAG, message, e);
        if (isEnabled && isInitied) {
            addLogRecord(new LogRecord(System.currentTimeMillis(), TAG, Thread.currentThread().getName(), message + "\n" +
                    android.util.Log.getStackTraceString(e)));
        }
    }

    public static void t(String TAG, Throwable e) {
        if (!ENABLED) {
            return;
        }
        Log.w("T|" + TAG, e);
        if (isEnabled && isInitied) {
            addLogRecord(new LogRecord(System.currentTimeMillis(), TAG, Thread.currentThread().getName(),
                    android.util.Log.getStackTraceString(e)));
        }
    }

    public static void w(String TAG, String message) {
        if (!ENABLED) {
            return;
        }
        if (LOG_THREAD) {
            Log.w("T|" + TAG, Thread.currentThread().getName() + "| " + message);
        } else {
            Log.w("T|" + TAG, message);
        }
        if (isEnabled && isInitied) {
            addLogRecord(new LogRecord(System.currentTimeMillis(), TAG, Thread.currentThread().getName(), message));
        }
    }

    public static void p(String TAG, String action, long duration) {
        Log.w("T|P|" + TAG, action + ": " + duration + " ms");
    }

    private static ThreadLocal<Long> time = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return SystemClock.uptimeMillis();
        }
    };

    public static void resetPerformanceTimer() {
        time.set(SystemClock.uptimeMillis());
    }

    public static void recordTme(String TAG, String action) {
        long duration = SystemClock.uptimeMillis() - time.get();
        time.set(SystemClock.uptimeMillis());
        p(TAG, action, duration);
    }

    public static String saveDump(byte[] data) {
        if (isEnabled) {
            String file = "/sdcard/dump_" + System.currentTimeMillis() + ".bin";
            try {
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(data, 0, data.length);
                stream.flush();
                stream.close();
                d(TAG, "Saved dump: " + file);
            } catch (FileNotFoundException e) {
                t(TAG, e);
            } catch (IOException e) {
                t(TAG, e);
            }
            return file;
        } else {
            return null;
        }
    }
}
