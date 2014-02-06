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
import java.util.HashSet;

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

    private static final boolean REUSE_BITMAPS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private QueueProcessor<AvatarTask> processor;
    private TelegramApplication application;
    private QueueWorker[] workers;
    private ArrayList<ReceiverHolder> receivers = new ArrayList<ReceiverHolder>();
    private AvatarCache avatarCache;
    private ImageStorage fileStorage;
    private Handler handler = new Handler(Looper.getMainLooper());

    private ThreadLocal<byte[]> bitmapTmp = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[16 * 1024];
        }
    };
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
        this.avatarCache = new AvatarCache();
        this.fileStorage = new ImageStorage(application, "avatars");
        this.processor = new QueueProcessor<AvatarTask>();

        float density = application.getResources().getDisplayMetrics().density;

        AVATAR_S_W = (int) (density * 42);
        AVATAR_S_H = (int) (density * 42);

        AVATAR_M_W = (int) (density * 52);
        AVATAR_M_H = (int) (density * 52);

        AVATAR_M2_W = (int) (density * 64);
        AVATAR_M2_H = (int) (density * 64);

        this.workers = new QueueWorker[]{
                new DownloadWorker(), new DownloadWorker(),
                new FileWorker(), new FileWorker()};

        for (QueueWorker w : workers) {
            w.start();
        }
    }

    private void checkUiThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalAccessError("Might be called on UI thread");
        }
    }

    public void requestAvatar(TLAbsLocalFileLocation fileLocation,
                              int kind,
                              AvatarReceiver receiver) {
        checkUiThread();

        String key = fileLocation.getUniqKey();
        Bitmap cached = avatarCache.getFromCache(key + "_" + kind);
        if (cached != null) {
            receiver.onAvatarReceived(cached, true);
            return;
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
        receivers.add(new ReceiverHolder(key, kind, receiver));
        processor.requestTask(new AvatarTask(fileLocation, kind));
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

    protected void notifyAvatarLoaded(final AvatarTask task, final int kind, final Bitmap bitmap) {
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
                            receiver.onAvatarReceived(bitmap, false);
                        }
                    }
                }
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

        byte[] sSize = Optimizer.save(sBitmap);
        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_SMALL, sSize);
        byte[] mSize = Optimizer.save(mBitmap);
        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_MEDIUM, mSize);
        byte[] m2Size = Optimizer.save(m2Bitmap);
        fileStorage.saveFile(task.getFileLocation().getUniqKey() + "_" + TYPE_MEDIUM2, m2Size);

        Bitmap mRes = Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
        Optimizer.drawTo(mBitmap, mRes);
        avatarCache.putToCache(task.getFileLocation().getUniqKey() + "_" + TYPE_MEDIUM, mRes);
        notifyAvatarLoaded(task, TYPE_MEDIUM, mRes);
    }

    private abstract class BaseWorker extends QueueWorker<AvatarTask> {
        protected byte[] bitmapTmp;

        public BaseWorker() {
            super(processor);

            bitmapTmp = new byte[16 * 1024];
        }
    }

    private boolean processPreTask(AvatarTask task) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 1;
        o.inScaled = false;
        o.inTempStorage = bitmapTmp.get();
        if (Build.VERSION.SDK_INT >= 10) {
            o.inPreferQualityOverSpeed = false;
        }

//            if (REUSE_BITMAPS) {
//                o.inBitmap = Bit fullBitmaps.get();
//            }

        Bitmap res = fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + task.getKind(), o);
        if (res != null) {
            avatarCache.putToCache(task.getFileLocation().getUniqKey() + "_" + task.getKind(), res);
            notifyAvatarLoaded(task, task.getKind(), res);
            return true;
        }


        if (task.getKind() != TYPE_FULL) {
            if (REUSE_BITMAPS) {
                o.inMutable = true;
                o.inBitmap = fullBitmaps.get();
            }

            Bitmap fullBitmap = fileStorage.tryLoadFile(task.getFileLocation().getUniqKey() + "_" + TYPE_FULL, o);
            if (fullBitmap != null) {
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
        protected boolean processTask(AvatarTask task) {
            boolean pre = processPreTask(task);
            if (pre) {
                return true;
            } else {
                synchronized (task) {
                    task.setDownloaded(false);
                }
                return false;
            }
        }

        @Override
        public boolean isAccepted(AvatarTask task) {
            return task.isDownloaded();
        }
    }

    private class DownloadWorker extends BaseWorker {
        @Override
        protected boolean processTask(AvatarTask task) {
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

            try {
                TLFile res = application.getApi().doGetFile(dcId, location, 0, 1024 * 1024 * 1024);

                fileStorage.saveFile(fileLocation.getUniqKey() + "_" + TYPE_FULL, res.getBytes());

                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 1;
                o.inScaled = false;
                o.inTempStorage = bitmapTmp;
                if (Build.VERSION.SDK_INT >= 10) {
                    o.inPreferQualityOverSpeed = false;
                }

                if (REUSE_BITMAPS) {
                    o.inMutable = true;
                    o.inBitmap = fullBitmaps.get();
                }

                Bitmap src = BitmapFactory.decodeByteArray(res.getBytes(), 0, res.getBytes().length, o);

                onFullBitmapLoaded(src, task);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        public boolean isAccepted(AvatarTask task) {
            return true;
        }
    }

    private class ReceiverHolder {
        private String key;
        private int kind;
        private WeakReference<AvatarReceiver> receiverReference;

        private ReceiverHolder(String key, int kind, AvatarReceiver receiverReference) {
            this.key = key;
            this.kind = kind;
            this.receiverReference = new WeakReference<AvatarReceiver>(receiverReference);
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
        private boolean isPaused = false;
        private int kind;

        private AvatarTask(TLAbsLocalFileLocation fileLocation, int kind) {
            this.fileLocation = fileLocation;
            this.kind = kind;
        }

        public boolean isPaused() {
            return isPaused;
        }

        public void setPaused(boolean isPaused) {
            this.isPaused = isPaused;
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
