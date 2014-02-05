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

    private static final boolean REUSE_BITMAPS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private QueueProcessor<AvatarTask> processor;
    private TelegramApplication application;
    private DownloadWorker[] downloadWorkers;
    private ArrayList<ReceiverHolder> receivers = new ArrayList<ReceiverHolder>();
    private Handler handler = new Handler(Looper.getMainLooper());

    public AvatarLoader(TelegramApplication application) {
        this.application = application;
        this.processor = new QueueProcessor<AvatarTask>();

        float density = application.getResources().getDisplayMetrics().density;

        AVATAR_S_W = (int) (density * 42);
        AVATAR_S_H = (int) (density * 42);

        AVATAR_M_W = (int) (density * 52);
        AVATAR_M_H = (int) (density * 52);

        AVATAR_M2_W = (int) (density * 64);
        AVATAR_M2_H = (int) (density * 64);

        this.downloadWorkers = new DownloadWorker[]{new DownloadWorker(), new DownloadWorker()};

        for (DownloadWorker w : downloadWorkers) {
            w.start();
        }
    }

    private void checkUiThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalAccessError("Might be called on UI thread");
        }
    }

    public void requestAvatar(int peerType, int peerId, TLAbsLocalFileLocation fileLocation,
                              AvatarReceiver receiver) {
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
        receivers.add(new ReceiverHolder(peerType, peerId, TYPE_MEDIUM, receiver));
        processor.requestTask(new AvatarTask(peerType, peerId, fileLocation));
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

    protected void notifyAvatarLoaded(final AvatarTask task, final Bitmap bitmap) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ReceiverHolder holder : receivers.toArray(new ReceiverHolder[0])) {
                    if (holder.getReceiverReference().get() == null) {
                        receivers.remove(holder);
                        continue;
                    }
                    if (holder.getPeerType() == task.getPeerType() && holder.getPeerId() == task.getPeerId()) {
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

    private class DownloadWorker extends QueueWorker<AvatarTask> {
        private Bitmap reuseBitmap;

        private Bitmap sBitmap;
        private Bitmap mBitmap;
        private Bitmap m2Bitmap;

        private byte[] bitmapTmp;

        public DownloadWorker() {
            super(processor);

            bitmapTmp = new byte[16 * 1024];

            sBitmap = Bitmap.createBitmap(AVATAR_S_W, AVATAR_S_H, Bitmap.Config.ARGB_8888);
            mBitmap = Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
            m2Bitmap = Bitmap.createBitmap(AVATAR_M2_W, AVATAR_M2_H, Bitmap.Config.ARGB_8888);
            if (REUSE_BITMAPS) {
                reuseBitmap = Bitmap.createBitmap(AVATAR_W, AVATAR_H, Bitmap.Config.ARGB_8888);
            }
        }

        @Override
        protected boolean processTask(AvatarTask task) {
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

                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 1;
                o.inScaled = false;
                o.inTempStorage = bitmapTmp;
                if (Build.VERSION.SDK_INT >= 10) {
                    o.inPreferQualityOverSpeed = false;
                }

                if (REUSE_BITMAPS) {
                    o.inBitmap = reuseBitmap;
                }

                Bitmap src = BitmapFactory.decodeByteArray(res.getBytes(), 0, res.getBytes().length, o);

                // Prepare thumbs
                Optimizer.scaleTo(src, sBitmap);
                Optimizer.scaleTo(src, mBitmap);
                Optimizer.scaleTo(src, m2Bitmap);

                byte[] original = res.getBytes();
                byte[] sSize = Optimizer.save(sBitmap);
                byte[] mSize = Optimizer.save(mBitmap);
                byte[] m2Size = Optimizer.save(m2Bitmap);

                Bitmap mRes = Bitmap.createBitmap(AVATAR_M_W, AVATAR_M_H, Bitmap.Config.ARGB_8888);
                Optimizer.drawTo(mBitmap, mRes);
                notifyAvatarLoaded(task, mRes);
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
        private int peerType;
        private int peerId;
        private int kind;
        private WeakReference<AvatarReceiver> receiverReference;

        private ReceiverHolder(int peerType, int peerId, int kind, AvatarReceiver receiverReference) {
            this.peerType = peerType;
            this.peerId = peerId;
            this.kind = kind;
            this.receiverReference = new WeakReference<AvatarReceiver>(receiverReference);
        }

        public int getPeerType() {
            return peerType;
        }

        public int getPeerId() {
            return peerId;
        }

        public int getKind() {
            return kind;
        }

        public WeakReference<AvatarReceiver> getReceiverReference() {
            return receiverReference;
        }
    }

    private class AvatarTask extends QueueProcessor.BaseTask {
        private int peerType;
        private int peerId;
        private TLAbsLocalFileLocation fileLocation;

        private AvatarTask(int peerType, int peerId, TLAbsLocalFileLocation fileLocation) {
            this.peerType = peerType;
            this.peerId = peerId;
            this.fileLocation = fileLocation;
        }

        public int getPeerType() {
            return peerType;
        }

        public int getPeerId() {
            return peerId;
        }

        public TLAbsLocalFileLocation getFileLocation() {
            return fileLocation;
        }

        @Override
        public long getKey() {
            return peerId * 10L + peerType;
        }
    }
}
