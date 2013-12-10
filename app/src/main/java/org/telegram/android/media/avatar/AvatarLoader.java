package org.telegram.android.media.avatar;

import android.graphics.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.log.Logger;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.upload.TLFile;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class AvatarLoader {

    private static final String TAG = "AvatarLoader";

    private static final AtomicInteger idCounter = new AtomicInteger(1);

    private static final int MAX_CACHE_SIZE = 15;

    private byte[] tempStorage = new byte[32 * 1024];

    private HashMap<String, byte[]> requestCache;

    private HashMap<String, byte[]> avatarCache = new HashMap<String, byte[]>();

    private ArrayList<Bitmap> cache = new ArrayList<Bitmap>();
    private ArrayList<Bitmap> cacheFull = new ArrayList<Bitmap>();

    private HandlerThread networkLoader;

    private StelsApplication application;

    private ConcurrentHashMap<Integer, Task> tasks;
    private ConcurrentHashMap<String, Task> taskKeys;

    private Handler networkHandler;

    private Handler mainHandler;

    private Paint paint;

    public AvatarLoader(StelsApplication application) {
        this.application = application;
        this.requestCache = new HashMap<String, byte[]>();
        this.tasks = new ConcurrentHashMap<Integer, Task>();
        this.taskKeys = new ConcurrentHashMap<String, Task>();
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.paint.setAntiAlias(true);
        this.networkLoader = new HandlerThread("AvatarNetworkLoader", Thread.MIN_PRIORITY);
        this.networkLoader.start();
        while (this.networkLoader.getLooper() == null) {
            Thread.yield();
        }
        this.networkHandler = new Handler(networkLoader.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    Task task = tasks.get(msg.what);
                    if (task == null) {
                        return;
                    }
                    doNetworkTask(task);
                } catch (Exception e) {
                    e.printStackTrace();
                    networkHandler.sendEmptyMessageDelayed(msg.what, 5000);
                }
            }
        };
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized void disconnectSource(AvatarHolder holder, int id) {
        Task task = tasks.get(id);
        if (task != null) {
            for (WeakReference<AvatarHolder> h : task.holders.toArray(new WeakReference[0])) {
                if (h.get() == holder) {
                    task.holders.remove(h);
                }
            }
        }

        Bitmap bitmap = holder.getAvatar();
        if (bitmap != null) {
            if (cache.size() < MAX_CACHE_SIZE) {
                cache.add(bitmap);
            }
        }
        holder.clearAvatar();
    }

    public synchronized int connectSource(AvatarHolder holder, TLLocalFileLocation fileLocation, int size) {
        String key = size + ":" + fileLocation.getDcId() + "+" + fileLocation.getVolumeId() + "+" + fileLocation.getLocalId();
        if (taskKeys.containsKey(key)) {
            Task res = taskKeys.get(key);
            res.holders.add(new WeakReference<AvatarHolder>(holder));
            return res.id;
        }
        final Task task = new Task();
        task.id = idCounter.incrementAndGet();
        task.location = fileLocation;
        task.size = size;
        task.taskKey = key;
        task.holders.add(new WeakReference<AvatarHolder>(holder));

        tasks.put(task.id, task);
        taskKeys.put(task.taskKey, task);

        // networkHandler.sendEmptyMessageDelayed(task.id, 150);

//        if (avatarCache.containsKey(task.taskKey)) {
//            byte[] data = avatarCache.get(task.taskKey);
//
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inMutable = true;
//            options.inSampleSize = 1;
//            options.inTempStorage = tempStorage;
//
//            if (cacheFull.size() > 0) {
//                options.inBitmap = cacheFull.get(0);
//                cacheFull.remove(0);
//            }
//
//            final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
//
//            mainHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    notifyTask(task, bmp);
//                }
//            });
//        } else {
//            networkHandler.sendEmptyMessageDelayed(task.id, 150);
//        }

        networkHandler.sendEmptyMessageDelayed(task.id, 100);

        return task.id;
    }

    private Bitmap prepareBitmap(byte[] data) {
        long start = System.currentTimeMillis();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = 1;
        options.inTempStorage = tempStorage;

        if (cacheFull.size() > 0) {
            options.inBitmap = cacheFull.get(0);
            cacheFull.remove(0);
        }

        final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        Logger.d(TAG, "First bitmap decoded in " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();

        final Bitmap src;
        if (cache.size() > 0) {
            src = cache.get(0);
            cache.remove(0);
        } else {
            src = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(src);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawBitmap(bmp, new Rect(0, 0, 160, 160), new Rect(0, 0, 96, 96), paint);

        if (options.inBitmap != null) {
            if (cacheFull.size() < MAX_CACHE_SIZE) {
                cacheFull.add(options.inBitmap);
            }
        }

        Logger.d(TAG, "Second bitmap decoded in " + (System.currentTimeMillis() - start) + " ms");
        return src;
    }


    protected synchronized void notifyTask(Task task, Bitmap bmp) {
        boolean hasNotifications = false;
        WeakReference<AvatarHolder>[] holders = task.holders.toArray(new WeakReference[0]);
        for (WeakReference<AvatarHolder> holder : holders) {
            AvatarHolder avatarHolder = holder.get();
            if (avatarHolder != null) {
                if (avatarHolder.getSourceId() != task.id) {
                    continue;
                }
                hasNotifications = true;
                avatarHolder.onAvatarLoaded(bmp);
            }
        }
        tasks.remove(task.id);
        taskKeys.remove(task.taskKey);
        if (!hasNotifications) {
            if (cache.size() < MAX_CACHE_SIZE) {
                cache.add(bmp);
            }
        }
    }

    protected void doNetworkTask(final Task task) throws Exception {
        TLLocalFileLocation fileLocation = task.location;

        if (avatarCache.containsKey(task.taskKey)) {
            byte[] data = avatarCache.get(task.taskKey);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            options.inSampleSize = 1;
            options.inTempStorage = tempStorage;

            if (cache.size() > 0) {
                options.inBitmap = cache.get(0);
                cache.remove(0);
            }

            final Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyTask(task, bmp);
                }
            });
            return;
        }

        byte[] data;
        if (requestCache.containsKey(task.taskKey)) {
            data = requestCache.get(task.taskKey);
        } else {
            TLFile res = application.getApi().doGetFile(fileLocation.getDcId(), new TLInputFileLocation(
                    fileLocation.getVolumeId(),
                    fileLocation.getLocalId(),
                    fileLocation.getSecret()),
                    0, 1024 * 1024 * 1024);
            data = res.getBytes();
            requestCache.put(task.taskKey, data);
        }

        final Bitmap src = prepareBitmap(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
        avatarCache.put(task.taskKey, outputStream.toByteArray());

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyTask(task, src);
            }
        });
    }

    private class Task {
        public int id;
        public String taskKey;
        public int size;
        public TLLocalFileLocation location;
        public HashSet<WeakReference<AvatarHolder>> holders = new HashSet<WeakReference<AvatarHolder>>();
    }
}