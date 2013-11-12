package org.telegram.android.core.files;

import android.os.SystemClock;
import android.util.Pair;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;
import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.tl.TLBool;
import org.telegram.tl.TLObject;

import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.*;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 16:50
 */
public class UploadController {

    private static final String TAG = "Uploader";

    public interface UploadListener {
        public void onProgressChanged(int percent);
    }


    private static final int ATTEMPTS_COUNT = 3;

    private static final int THREADS_COUNT = 2;

    private static final int NOTIFY_DELAY = 500;
    private static final int TIMEOUT = 30000;

    private static final int PAGE_SIZE = 128 * 1024;
    private static final int PAGE_SIZE_SLOW = 8 * 1024;

    private StelsApplication application;

    private ExecutorService service = Executors.newFixedThreadPool(THREADS_COUNT);

    public UploadController(StelsApplication application) {
        this.application = application;
    }

    public UploadResult uploadFile(InputStream inputStream, int len, long fileId) {
        return uploadFile(inputStream, len, fileId, null);
    }

    public UploadResult uploadFile(InputStream inputStream, int len, long fileId, UploadListener listener) {
        try {

            long start = SystemClock.uptimeMillis();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            MessageDigest crypt = MessageDigest.getInstance("MD5");
            crypt.reset();

            int part = 0;


            long lastProgressNotify = SystemClock.currentThreadTimeMillis();

            int offset = 0;
            int downloaded = 0;

            CopyOnWriteArrayList<Pair<Future<Boolean>, Integer>> active = new CopyOnWriteArrayList<Pair<Future<Boolean>, Integer>>();

            while (offset < len) {
                int packetSize = PAGE_SIZE_SLOW;
                byte[] block;
                if (len - offset < packetSize) {
                    block = new byte[len - offset];
                } else {
                    block = new byte[packetSize];
                }
                bufferedInputStream.read(block);
                crypt.update(block);
                part++;
                offset += packetSize;

                Future<Boolean> res = service.submit(new UploadPartTask(fileId, part - 1, block));
                active.add(new Pair<Future<Boolean>, Integer>(res, block.length));

                int count;
                do {
                    count = 0;
                    for (Pair<Future<Boolean>, Integer> task : active) {
                        if (!task.first.isDone()) {
                            count++;
                        } else {
                            active.remove(task);
                            Boolean bool = task.first.get();
                            if (bool == null || !bool) {
                                throw new IOException();
                            }
                            downloaded += task.second;
                            if (listener != null) {
                                if (SystemClock.uptimeMillis() - lastProgressNotify > NOTIFY_DELAY) {
                                    listener.onProgressChanged(100 * downloaded / len);
                                    lastProgressNotify = SystemClock.uptimeMillis();
                                }
                            }
                        }
                    }
                    if (count >= THREADS_COUNT) {
                        Thread.sleep(10);
                    }
                } while (count >= THREADS_COUNT);
            }

            for (Pair<Future<Boolean>, Integer> task : active) {
                Boolean bool = task.first.get();
                if (bool == null || !bool) {
                    throw new IOException();
                }
                downloaded += task.second;
                if (listener != null) {
                    if (SystemClock.uptimeMillis() - lastProgressNotify > NOTIFY_DELAY) {
                        listener.onProgressChanged(100 * downloaded / len);
                        lastProgressNotify = SystemClock.uptimeMillis();
                    }
                }
            }

            if (listener != null) {
                listener.onProgressChanged(100);
            }

            Logger.d(TAG, "UploadTime: " + (SystemClock.uptimeMillis() - start) + " ms");

            return new UploadResult(CryptoUtils.ToHex(crypt.digest()), part);
        } catch (Exception e) {
            Logger.t(TAG, e);
        }
        return null;
    }

    private class UploadPartTask implements Callable<Boolean> {

        private long fileId;
        private int part;
        private byte[] data;

        public UploadPartTask(long fileId, int part, byte[] data) {
            this.fileId = fileId;
            this.part = part;
            this.data = data;
        }

        @Override
        public Boolean call() throws Exception {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            int attempt = 0;
            while (attempt++ < ATTEMPTS_COUNT) {
                return application.getApi().doSaveFilePart(fileId, part, data);
            }
            return false;
        }
    }
}
