package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.media.BitmapDecoderEx;
import org.telegram.android.media.OptimizedBlur;
import org.telegram.android.media.Optimizer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 08.02.14.
 */
public class MediaLoader {
    private ImageCache cache;
    private TelegramApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ArrayList<ReceiverHolder> receivers = new ArrayList<ReceiverHolder>();
    private QueueWorker[] workers;
    private QueueProcessor<BaseTask> processor;
    private ImageCache imageCache;

    private Bitmap fullImageCached = null;
    private Object fullImageCachedLock = new Object();

    private final int PREVIEW_MAX_W;
    private final int PREVIEW_MAX_H;

    private ThreadLocal<byte[]> bitmapTmp = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[16 * 1024];
        }
    };

    public MediaLoader(TelegramApplication application) {
        this.application = application;
        this.processor = new QueueProcessor<BaseTask>();
        this.imageCache = new ImageCache();

        float density = application.getResources().getDisplayMetrics().density;

        PREVIEW_MAX_W = (int) (density * 160);
        PREVIEW_MAX_H = (int) (density * 300);

        this.workers = new QueueWorker[]{
                new FastWorker(),
                new FileWorker()
        };

        for (QueueWorker w : workers) {
            w.start();
        }
    }

    public void requestFastLoading(TLLocalPhoto photo, MediaReceiver receiver) {
        checkUiThread();

        String key = photo.getFastPreviewKey();
        Bitmap cached = imageCache.getFromCache(key);
        if (cached != null) {
            receiver.onMediaReceived(cached, photo.getFastPreviewW(), photo.getFastPreviewH(), key, true);
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
        receivers.add(new ReceiverHolder(key, receiver));

        processor.requestTask(new MediaFastTask(photo));
    }

    public void requestFullLoading(TLLocalPhoto photo, String fileName, MediaReceiver receiver) {
        checkUiThread();

        String key = fileName;
        Bitmap cached = imageCache.getFromCache(key);
        if (cached != null) {
            receiver.onMediaReceived(cached, photo.getFullW(), photo.getFullH(), key, true);
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
        receivers.add(new ReceiverHolder(key, receiver));

        processor.requestTask(new MediaFileTask(photo, fileName));
    }

    public void cancelRequest(MediaReceiver receiver) {
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

    public ImageCache getImageCache() {
        return imageCache;
    }

    private void checkUiThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalAccessError("Might be called on UI thread");
        }
    }

    protected void notifyMediaLoaded(final QueueProcessor.BaseTask task, final Bitmap bitmap, final int regionW, final int regionH) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (ReceiverHolder holder : receivers.toArray(new ReceiverHolder[0])) {
                    if (holder.getReceiverReference().get() == null) {
                        receivers.remove(holder);
                        continue;
                    }
                    if (holder.getKey().equals(task.getKey())) {
                        receivers.remove(holder);
                        MediaReceiver receiver = holder.getReceiverReference().get();
                        if (receiver != null) {
                            receiver.onMediaReceived(bitmap, regionW, regionH, task.getKey(), false);
                        }
                    }
                }
            }
        });
    }


    private class ReceiverHolder {
        private String key;
        private WeakReference<MediaReceiver> receiverReference;

        private ReceiverHolder(String key, MediaReceiver receiverReference) {
            this.key = key;
            this.receiverReference = new WeakReference<MediaReceiver>(receiverReference);
        }

        public String getKey() {
            return key;
        }

        public WeakReference<MediaReceiver> getReceiverReference() {
            return receiverReference;
        }
    }

    private abstract class BaseTask extends QueueProcessor.BaseTask {

    }

    private class MediaFileTask extends BaseTask {

        private TLLocalPhoto localPhoto;
        private String fileName;

        public MediaFileTask(TLLocalPhoto localPhoto, String fileName) {
            this.localPhoto = localPhoto;
            this.fileName = fileName;
        }

        public TLLocalPhoto getLocalPhoto() {
            return localPhoto;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public String getKey() {
            return fileName;
        }
    }

    private class MediaFastTask extends BaseTask {

        private TLLocalPhoto photo;

        private MediaFastTask(TLLocalPhoto photo) {
            this.photo = photo;
        }

        public TLLocalPhoto getPhoto() {
            return photo;
        }

        @Override
        public String getKey() {
            return photo.getFastPreviewKey();
        }
    }

    private class FastWorker extends QueueWorker<BaseTask> {

        private OptimizedBlur optimizedBlur = new OptimizedBlur();

        public FastWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(BaseTask task) {
            if (!(task instanceof MediaFastTask)) {
                return false;
            }

            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

            TLLocalPhoto mediaPhoto = ((MediaFastTask) task).getPhoto();

            bitmapOptions.inSampleSize = 1;
            bitmapOptions.inScaled = false;
            bitmapOptions.inTempStorage = bitmapTmp.get();
            if (Build.VERSION.SDK_INT >= 10) {
                bitmapOptions.inPreferQualityOverSpeed = false;
            }

            // BitmapDecoderEx.decodeReuseBitmap();
            Bitmap img = BitmapFactory.decodeByteArray(mediaPhoto.getFastPreview(), 0, mediaPhoto.getFastPreview().length, bitmapOptions);
            if ((mediaPhoto.getOptimization() & TLLocalPhoto.OPTIMIZATION_BLUR) == 0) {
                optimizedBlur.performBlur(img);
            }
            notifyMediaLoaded(task, img, mediaPhoto.getFastPreviewW(), mediaPhoto.getFastPreviewH());
//            boolean isReused = false;
//            if (Build.VERSION.SDK_INT >= 11) {
//                bitmapOptions.inMutable = true;
//                Bitmap destBitmap;
//                if ((mediaPhoto.getOptimization() & TLLocalPhoto.OPTIMIZATION_RESIZE) != 0) {
//                    destBitmap = fastCache.findInFreeCache(TLLocalPhoto.FAST_PREVIEW_MAX_W, TLLocalPhoto.FAST_PREVIEW_MAX_H);
//                } else {
//                    destBitmap = fastCache.findInFreeCache(mediaPhoto.getFastPreviewW(), mediaPhoto.getFastPreviewH());
//                }
//                if (destBitmap != null) {
//                    bitmapOptions.inBitmap = destBitmap;
//                    isReused = true;
//                } else {
//                    bitmapOptions.inBitmap = null;
//                }
//            }
//
//            long start = SystemClock.uptimeMillis();
//            Bitmap img = BitmapFactory.decodeByteArray(mediaPhoto.getFastPreview(), 0, mediaPhoto.getFastPreview().length, bitmapOptions);
//            if ((mediaPhoto.getOptimization() & TLLocalPhoto.OPTIMIZATION_BLUR) == 0) {
//                optimizedBlur.performBlur(img);
//            }
//
//            previewCached = img;

            return true;
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaFastTask;
        }
    }

    private class FileWorker extends QueueWorker<BaseTask> {

        private FileWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(BaseTask task) {
            if (!(task instanceof MediaFileTask)) {
                return false;
            }

            MediaFileTask fileTask = (MediaFileTask) task;

            synchronized (fullImageCachedLock) {
                if (fullImageCached == null) {
                    fullImageCached = Bitmap.createBitmap(ApiUtils.MAX_SIZE, ApiUtils.MAX_SIZE, Bitmap.Config.ARGB_8888);
                }

                fullImageCached.eraseColor(Color.TRANSPARENT);

                BitmapDecoderEx.decodeReuseBitmap(fileTask.fileName, fullImageCached);

                Bitmap res = Bitmap.createBitmap(PREVIEW_MAX_W, PREVIEW_MAX_H, Bitmap.Config.ARGB_8888);
                int[] sizes = Optimizer.scaleToRatio(fullImageCached,
                        fileTask.getLocalPhoto().getFullW(), fileTask.getLocalPhoto().getFullH(), res);

                notifyMediaLoaded(task, res, sizes[0], sizes[1]);
            }
            return true;
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaFileTask;
        }
    }
}
