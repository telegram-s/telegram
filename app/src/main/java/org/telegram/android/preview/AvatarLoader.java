package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.Optimizer;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.upload.TLFile;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 05.02.14.
 */
public class AvatarLoader {

    private static final int AVATAR_W = 160;
    private static final int AVATAR_H = 160;

    private final int AVATAR_S_W;
    private final int AVATAR_S_H;

    private final int AVATAR_M_W;
    private final int AVATAR_M_H;

    private final int AVATAR_M2_W;
    private final int AVATAR_M2_H;

    public static final int TYPE_FULL = 0;
    public static final int TYPE_MEDIUM = 1;
    public static final int TYPE_MEDIUM2 = 2;
    public static final int TYPE_SMALL = 3;

    private QueueProcessor<AvatarTask> processor;
    private TelegramApplication application;
    private QueueWorker[] workers;
    private ArrayList<ReceiverHolder> receivers = new ArrayList<ReceiverHolder>();
    private ImageCache imageCache;
    private ImageStorage fileStorage;
    private Handler handler = new Handler(Looper.getMainLooper());

    private ThreadLocal<Bitmap> sBitmaps = new ThreadLocal<Bitmap>() {
        @Override
        protected Bitmap initialValue() {
            return Bitmap.createBitmap(AVATAR_S_W, AVATAR_S_H, Bitmap.Config.ARGB_8888);
        }
    };
    private ThreadLocal<Bitmap> mBitmaps = new ThreadLocal<Bitmap>() {
        @Override
        protected Bitmap initialValue() {
            return Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
        }
    };
    private ThreadLocal<Bitmap> m2Bitmaps = new ThreadLocal<Bitmap>() {
        @Override
        protected Bitmap initialValue() {
            return Bitmap.createBitmap(AVATAR_M2_W, AVATAR_M2_H, Bitmap.Config.ARGB_8888);
        }
    };
    private ThreadLocal<Bitmap> fullBitmaps = new ThreadLocal<Bitmap>() {
        @Override
        protected Bitmap initialValue() {
            return Bitmap.createBitmap(AVATAR_W, AVATAR_H, Bitmap.Config.ARGB_8888);
        }
    };

    public AvatarLoader(TelegramApplication application) {
        this.application = application;
        this.imageCache = new ImageCache();
        this.fileStorage = new ImageStorage(application, "avatars");
        this.processor = new QueueProcessor<AvatarTask>();

        float density = application.getResources().getDisplayMetrics().density;

        AVATAR_S_W = (int) (density * 42);
        AVATAR_S_H = (int) (density * 42);

        AVATAR_M_W = (int) (density * 54);
        AVATAR_M_H = (int) (density * 54);

        AVATAR_M2_W = (int) (density * 64);
        AVATAR_M2_H = (int) (density * 64);

        this.workers = new QueueWorker[]{
                new DownloadWorker(), new DownloadWorker(),
                new FileWorker(), new FileWorker()};

        for (QueueWorker w : workers) {
            w.start();
        }
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    private void checkUiThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalAccessError("Might be called on UI thread");
        }
    }

    public boolean requestAvatar(TLAbsLocalFileLocation fileLocation,
                                 int kind,
                                 AvatarReceiver receiver) {
        checkUiThread();

        String key = fileLocation.getUniqKey();
        String cacheKey = key + "_" + kind;
        BitmapHolder cached = imageCache.getFromCache(cacheKey);
        if (cached != null) {
            receiver.onAvatarReceived(new AvatarHolder(cached, this), true);
            return true;
        }

        for (ReceiverHolder holder : receivers.toArray(new ReceiverHolder[0])) {
            if (holder.getReceiverReference().get() == null) {
                receivers.remove(holder);
                continue;
            }
            if (holder.getReceiverReference().get() == receiver) {
                receivers.remove(holder);
                break;
            }
        }
        receivers.add(new ReceiverHolder(key, cacheKey, kind, receiver));
        processor.requestTask(new AvatarTask(fileLocation, kind));

        return false;
    }

    public void cancelRequest(AvatarReceiver receiver) {
        checkUiThread();
        for (ReceiverHolder holder : receivers.toArray(new ReceiverHolder[0])) {
            if (holder.getReceiverReference().get() == null) {
                receivers.remove(holder);
                continue;
            }
            if (holder.getReceiverReference().get() == receiver) {
                receivers.remove(holder);
                break;
            }
        }
    }

    protected void notifyAvatarLoaded(final AvatarTask task, final int kind, final BitmapHolder bitmap) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ReceiverHolder holder : receivers.toArray(new ReceiverHolder[0])) {
                    if (holder.getReceiverReference().get() == null) {
                        receivers.remove(holder);
                        continue;
                    }
                    if (holder.getKey().equals(task.getFileLocation().getUniqKey()) && holder.getKind() == kind) {
                        receivers.remove(holder);
                        AvatarReceiver receiver = holder.getReceiverReference().get();
                        if (receiver != null) {
                            receiver.onAvatarReceived(new AvatarHolder(bitmap, AvatarLoader.this), false);
                        }
                    }
                }
                imageCache.decReference(task.getKey(), AvatarLoader.this);
            }
        });
    }

    private void onFullBitmapLoaded(Bitmap src, AvatarTask task) throws IOException {
        Bitmap sBitmap = sBitmaps.get();
        Bitmap mBitmap = mBitmaps.get();
        Bitmap m2Bitmap = m2Bitmaps.get();

        // Prepare thumbs
        Optimizer.scaleTo(src, sBitmap);
        Optimizer.scaleTo(src, mBitmap);
        Optimizer.scaleTo(src, m2Bitmap);

        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_SMALL, sBitmap);
        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_MEDIUM, mBitmap);
        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_MEDIUM2, m2Bitmap);

        Bitmap mRes;
        if (task.getKind() == TYPE_FULL) {
            mRes = imageCache.findFree(TYPE_FULL);
            if (mRes == null) {
                mRes = Bitmap.createBitmap(AVATAR_W, AVATAR_H, Bitmap.Config.ARGB_8888);
            }
            Optimizer.drawTo(src, mRes);
        } else if (task.getKind() == TYPE_MEDIUM) {
            mRes = imageCache.findFree(TYPE_MEDIUM);
            if (mRes == null) {
                mRes = Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
            }
            Optimizer.drawTo(mBitmap, mRes);
        } else if (task.getKind() == TYPE_MEDIUM2) {
            mRes = imageCache.findFree(TYPE_MEDIUM2);
            if (mRes == null) {
                mRes = Bitmap.createBitmap(AVATAR_M2_W, AVATAR_M2_H, Bitmap.Config.ARGB_8888);
            }
            Optimizer.drawTo(m2Bitmap, mRes);
        } else if (task.getKind() == TYPE_SMALL) {
            mRes = imageCache.findFree(TYPE_SMALL);
            if (mRes == null) {
                mRes = Bitmap.createBitmap(AVATAR_S_W, AVATAR_S_H, Bitmap.Config.ARGB_8888);
            }
            Optimizer.drawTo(sBitmap, mRes);
        } else {
            return;
        }
        BitmapHolder holder = new BitmapHolder(mRes, task.getFileLocation().getUniqKey() + "_" + task.getKind());
        imageCache.putToCache(task.getKind(), holder, AvatarLoader.this);
        notifyAvatarLoaded(task, task.getKind(), holder);
    }

    private abstract class BaseWorker extends QueueWorker<AvatarTask> {
        protected byte[] bitmapTmp;

        public BaseWorker() {
            super(processor);

            bitmapTmp = new byte[16 * 1024];
        }
    }

    private boolean processPreTask(AvatarTask task) {
        Bitmap cached = imageCache.findFree(task.getKind());

        if (cached == null) {
            if (task.getKind() == TYPE_FULL) {
                cached = Bitmap.createBitmap(AVATAR_W, AVATAR_H, Bitmap.Config.ARGB_8888);
            } else if (task.getKind() == TYPE_SMALL) {
                cached = Bitmap.createBitmap(AVATAR_S_W, AVATAR_S_H, Bitmap.Config.ARGB_8888);
            } else if (task.getKind() == TYPE_MEDIUM) {
                cached = Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
            } else if (task.getKind() == TYPE_MEDIUM2) {
                cached = Bitmap.createBitmap(AVATAR_M2_W, AVATAR_M2_H, Bitmap.Config.ARGB_8888);
            }
        }
//        if (cached == null) {
//            res = fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + task.getKind());
//        } else {
//            res = fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + task.getKind(), cached);
//        }
        if (fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + task.getKind(), cached) != null) {
            BitmapHolder holder = new BitmapHolder(cached, task.getFileLocation().getUniqKey() + "_" + task.getKind());
            imageCache.putToCache(task.getKind(), holder, AvatarLoader.this);
            notifyAvatarLoaded(task, task.getKind(), holder);
            return true;
        }


        if (task.getKind() != TYPE_FULL) {
            Bitmap fullBitmap = fullBitmaps.get();
            Optimizer.BitmapInfo info = fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + TYPE_FULL, fullBitmap);
            if (info != null) {
                try {
                    onFullBitmapLoaded(fullBitmap, task);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        }

        return false;
    }

    private class FileWorker extends BaseWorker {

        @Override
        protected boolean processTask(AvatarTask task) throws Exception {
            boolean pre = processPreTask(task);
            if (!pre) {
                synchronized (task) {
                    task.setDownloaded(false);
                }
                return false;
            }

            return true;
        }

        @Override
        public boolean isAccepted(AvatarTask task) {
            return task.isDownloaded();
        }
    }

    private class DownloadWorker extends BaseWorker {
        @Override
        protected boolean processTask(AvatarTask task) throws Exception {
            boolean pre = processPreTask(task);
            if (pre) {
                return true;
            }

            TLAbsLocalFileLocation fileLocation = task.getFileLocation();
            TLInputFileLocation location;
            int dcId;
            if (fileLocation instanceof TLLocalFileLocation) {
                dcId = ((TLLocalFileLocation) fileLocation).getDcId();
                location = new TLInputFileLocation(
                        ((TLLocalFileLocation) fileLocation).getVolumeId(),
                        ((TLLocalFileLocation) fileLocation).getLocalId(),
                        ((TLLocalFileLocation) fileLocation).getSecret());
            } else {
                throw new UnsupportedOperationException();
            }

            TLFile res = application.getApi().doGetFile(dcId, location, 0, 1024 * 1024 * 1024);

            fileStorage.saveFile(fileLocation.getUniqKey() + "_" + TYPE_FULL, res.getBytes().cleanData());
            Bitmap src = fullBitmaps.get();
            Optimizer.loadTo(res.getBytes().cleanData(), src);
            onFullBitmapLoaded(src, task);

            return true;
        }

        @Override
        protected boolean needRepeatOnError() {
            return true;
        }

        @Override
        public boolean isAccepted(AvatarTask task) {
            return true;
        }
    }

    private class ReceiverHolder {
        private String key;
        private String cacheKey;
        private int kind;
        private WeakReference<AvatarReceiver> receiverReference;

        private ReceiverHolder(String key, String cacheKey, int kind, AvatarReceiver receiverReference) {
            this.key = key;
            this.cacheKey = cacheKey;
            this.kind = kind;
            this.receiverReference = new WeakReference<AvatarReceiver>(receiverReference);
        }

        public String getCacheKey() {
            return cacheKey;
        }

        public String getKey() {
            return key;
        }

        public int getKind() {
            return kind;
        }

        public WeakReference<AvatarReceiver> getReceiverReference() {
            return receiverReference;
        }
    }

    private class AvatarTask extends QueueProcessor.BaseTask {
        private TLAbsLocalFileLocation fileLocation;
        private boolean isDownloaded = true;
        private int kind;

        private AvatarTask(TLAbsLocalFileLocation fileLocation, int kind) {
            this.fileLocation = fileLocation;
            this.kind = kind;
        }

        public int getKind() {
            return kind;
        }

        public boolean isDownloaded() {
            return isDownloaded;
        }

        public void setDownloaded(boolean isDownloaded) {
            this.isDownloaded = isDownloaded;
        }

        public TLAbsLocalFileLocation getFileLocation() {
            return fileLocation;
        }

        @Override
        public String getKey() {
            return fileLocation.getUniqKey() + "_" + kind;
        }
    }
}
