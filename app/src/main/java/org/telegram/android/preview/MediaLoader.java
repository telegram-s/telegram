package org.telegram.android.preview;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.core.model.media.TLLocalDocument;
import org.telegram.android.core.model.media.TLLocalGeo;
import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;
import org.telegram.android.media.BitmapDecoderEx;
import org.telegram.android.media.Optimizer;
import org.telegram.android.media.VideoOptimizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by ex3ndr on 08.02.14.
 */
public class MediaLoader {

    private static final int SIZE_CHAT_PREVIEW = 0;
    private static final int SIZE_FAST_PREVIEW = 1;

    private TelegramApplication application;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ArrayList<ReceiverHolder> receivers = new ArrayList<ReceiverHolder>();
    private QueueWorker[] workers;
    private QueueProcessor<BaseTask> processor;
    private ImageCache imageCache;
    private ImageStorage imageStorage;

    private Bitmap fullImageCached = null;
    private final Object fullImageCachedLock = new Object();

    private final int PREVIEW_MAX_W;
    private final int PREVIEW_MAX_H;

    private final int MAP_W;
    private final int MAP_H;

    public MediaLoader(TelegramApplication application) {
        this.application = application;
        this.processor = new QueueProcessor<BaseTask>();
        this.imageCache = new ImageCache(2, 3);
        this.imageStorage = new ImageStorage(application, "previews");

        float density = application.getResources().getDisplayMetrics().density;

        PREVIEW_MAX_W = (int) (density * 160);
        PREVIEW_MAX_H = (int) (density * 300);

        MAP_H = (int) (density * 160);
        MAP_W = (int) (density * 160);

        this.workers = new QueueWorker[]{
                new FastWorker(),
                new FileWorker(),
                new MapWorker(),
                new RawWorker()
        };

        for (QueueWorker w : workers) {
            w.start();
        }
    }

    public void requestRaw(String fileName, MediaReceiver receiver) {
        requestTask(new MediaRawTask(fileName), fileName, receiver);
    }

    public void requestGeo(TLLocalGeo geo, MediaReceiver receiver) {
        String key = "geo:" + geo.getLatitude() + "," + geo.getLongitude();
        requestTask(new MediaGeoTask(geo.getLatitude(), geo.getLongitude()), key, receiver);
    }

    public void requestFastLoading(TLLocalPhoto photo, MediaReceiver receiver) {
        requestTask(new MediaPhotoFastTask(photo), photo.getFastPreviewKey(), receiver);
    }

    public void requestFastLoading(TLLocalDocument doc, MediaReceiver receiver) {
        requestTask(new MediaDocFastTask(doc), doc.getPreview().getUniqKey(), receiver);
    }

    public void requestFastLoading(TLLocalVideo video, MediaReceiver receiver) {
        requestTask(new MediaVideoFastTask(video), video.getPreviewKey(), receiver);
    }

    public void requestFullLoading(TLLocalPhoto photo, String fileName, MediaReceiver receiver) {
        requestTask(new MediaFileTask(photo, fileName), fileName, receiver);
    }

    public void requestVideoLoading(String fileName, MediaReceiver receiver) {
        requestTask(new MediaVideoTask(fileName), fileName, receiver);
    }

    private void requestTask(BaseTask task, String key, MediaReceiver receiver) {
        checkUiThread();

        if (checkCache(key, receiver)) {
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

        processor.requestTask(task);
    }

    private boolean checkCache(String key, MediaReceiver receiver) {
        BitmapHolder cached = imageCache.getFromCache(key);
        if (cached != null) {
            receiver.onMediaReceived(cached.getBitmap(), cached.getRealW(), cached.getRealH(), key, true);
            return true;
        }
        return false;
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
                imageCache.decReference(task.getKey(), MediaLoader.this);
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

    private class MediaGeoTask extends BaseTask {
        private double latitude;
        private double longitude;

        private MediaGeoTask(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        @Override
        public String getKey() {
            return "geo:" + latitude + "," + longitude;
        }
    }

    private class MediaPhotoFastTask extends BaseTask {

        private TLLocalPhoto photo;

        private MediaPhotoFastTask(TLLocalPhoto photo) {
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

    private class MediaDocFastTask extends BaseTask {

        private TLLocalDocument doc;

        private MediaDocFastTask(TLLocalDocument doc) {
            this.doc = doc;
        }

        public TLLocalDocument getDoc() {
            return doc;
        }

        @Override
        public String getKey() {
            return doc.getPreview().getUniqKey();
        }
    }

    private class MediaVideoFastTask extends BaseTask {

        private TLLocalVideo video;

        private MediaVideoFastTask(TLLocalVideo video) {
            this.video = video;
        }

        public TLLocalVideo getVideo() {
            return video;
        }

        @Override
        public String getKey() {
            return video.getPreviewKey();
        }
    }

    private class MediaVideoTask extends BaseTask {
        private String fileName;

        private MediaVideoTask(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public String getKey() {
            return fileName;
        }
    }

    private class MediaRawTask extends BaseTask {
        private String fileName;

        private MediaRawTask(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public String getKey() {
            return fileName;
        }
    }

    private class FastWorker extends QueueWorker<BaseTask> {

        public FastWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(BaseTask task) throws Exception {
            if (task instanceof MediaPhotoFastTask) {
                processPhoto((MediaPhotoFastTask) task);
            } else if (task instanceof MediaVideoFastTask) {
                processVideo((MediaVideoFastTask) task);
            } else if (task instanceof MediaDocFastTask) {
                processDoc((MediaDocFastTask) task);
            }
            return true;
        }

        private boolean processPhoto(MediaPhotoFastTask task) {
            TLLocalPhoto mediaPhoto = task.getPhoto();

            return process(
                    mediaPhoto.getFastPreview(),
                    mediaPhoto.getFastPreviewW(),
                    mediaPhoto.getFastPreviewH(),
                    mediaPhoto.getFastPreviewKey(),
                    task);
        }

        private boolean processVideo(MediaVideoFastTask task) {

            TLLocalVideo mediaVideo = task.getVideo();

            return process(
                    mediaVideo.getFastPreview(),
                    mediaVideo.getPreviewW(),
                    mediaVideo.getPreviewH(),
                    mediaVideo.getPreviewKey(),
                    task);
        }

        private boolean processDoc(MediaDocFastTask task) {
            TLLocalDocument mediaDoc = task.getDoc();

            return process(
                    mediaDoc.getFastPreview(),
                    mediaDoc.getPreviewW(),
                    mediaDoc.getPreviewH(),
                    mediaDoc.getPreview().getUniqKey(),
                    task);
        }

        private boolean process(byte[] data, int w, int h, String key, BaseTask task) {
            Bitmap img = imageCache.findFree(SIZE_FAST_PREVIEW);
            if (img != null) {
                try {
                    Optimizer.loadTo(data, img);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                try {
                    img = Optimizer.load(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            Optimizer.blur(img);
            // new OptimizedBlur().performBlur(img);
            // img = BitmapUtils.fastblur(img, 8);

            imageCache.putToCache(SIZE_FAST_PREVIEW, new BitmapHolder(img, key, w, h), MediaLoader.this);

            notifyMediaLoaded(task, img, w, h);

            return true;
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaPhotoFastTask || task instanceof MediaVideoFastTask || task instanceof MediaDocFastTask;
        }
    }

    private class RawWorker extends QueueWorker<BaseTask> {

        public RawWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(BaseTask task) throws Exception {
            if (task instanceof MediaRawTask) {
                processTask((MediaRawTask) task);
            }
            return true;
        }

        private boolean preprocessCached(MediaRawTask task, Bitmap src) {
            return imageStorage.tryLoadFile(task.getKey(), src) != null;
        }

        private void processTask(MediaRawTask rawTask) throws Exception {
            synchronized (fullImageCachedLock) {
                if (fullImageCached == null) {
                    fullImageCached = Bitmap.createBitmap(ApiUtils.MAX_SIZE / 2, ApiUtils.MAX_SIZE / 2, Bitmap.Config.ARGB_8888);
                }
            }

            Optimizer.BitmapInfo info = Optimizer.getInfo(rawTask.fileName);

            Bitmap res = imageCache.findFree(SIZE_CHAT_PREVIEW);
            if (res == null) {
                res = Bitmap.createBitmap(PREVIEW_MAX_W, PREVIEW_MAX_H, Bitmap.Config.ARGB_8888);
            } else {
                res.eraseColor(Color.TRANSPARENT);
            }

            if (info.getMimeType() != null && info.getMimeType().equals("image/jpeg")) {
                if (info.getHeight() <= fullImageCached.getHeight() && info.getWidth() <= fullImageCached.getWidth()) {
                    synchronized (fullImageCachedLock) {
                        BitmapDecoderEx.decodeReuseBitmap(rawTask.fileName, fullImageCached);
                        int[] sizes = Optimizer.scaleToRatio(fullImageCached, info.getWidth(), info.getHeight(), res);
                        imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, rawTask.getKey(), sizes[0], sizes[1]),
                                MediaLoader.this);
                        notifyMediaLoaded(rawTask, res, sizes[0], sizes[1]);
                    }
                } else if (info.getWidth() / 2 <= fullImageCached.getWidth() && info.getHeight() / 2 <= fullImageCached.getHeight()) {
                    synchronized (fullImageCachedLock) {
                        BitmapDecoderEx.decodeReuseBitmap(rawTask.fileName, fullImageCached);
                        int[] sizes = Optimizer.scaleToRatio(fullImageCached, info.getWidth() / 2, info.getHeight() / 2, res);
                        imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, rawTask.getKey(), sizes[0], sizes[1]),
                                MediaLoader.this);
                        notifyMediaLoaded(rawTask, res, sizes[0], sizes[1]);
                    }
                }
            }

            Bitmap tmp = Optimizer.optimize(rawTask.getFileName());
            int[] sizes = Optimizer.scaleToRatio(tmp, tmp.getWidth(), tmp.getHeight(), res);
            imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, rawTask.getKey(), sizes[0], sizes[1]),
                    MediaLoader.this);
            notifyMediaLoaded(rawTask, res, sizes[0], sizes[1]);
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaRawTask;
        }
    }

    private class FileWorker extends QueueWorker<BaseTask> {

        private FileWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(BaseTask task) throws Exception {
            if (task instanceof MediaFileTask) {
                processFileTask((MediaFileTask) task);
            } else if (task instanceof MediaVideoTask) {
                processVideoTask((MediaVideoTask) task);
            }
            return true;
        }

        private void processVideoTask(MediaVideoTask task) throws Exception {
            VideoOptimizer.VideoMetadata metadata = VideoOptimizer.getVideoSize(task.getFileName());

            Bitmap res = imageCache.findFree(SIZE_CHAT_PREVIEW);
            if (res == null) {
                res = Bitmap.createBitmap(PREVIEW_MAX_W, PREVIEW_MAX_H, Bitmap.Config.ARGB_8888);
            } else {
                res.eraseColor(Color.TRANSPARENT);
            }
            int[] sizes = Optimizer.scaleToRatio(metadata.getImg(),
                    metadata.getImg().getWidth(), metadata.getImg().getHeight(), res);

            imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, task.getKey(), sizes[0], sizes[1]),
                    MediaLoader.this);

            notifyMediaLoaded(task, res, sizes[0], sizes[1]);
        }

        private void processFileTask(MediaFileTask fileTask) throws Exception {
            synchronized (fullImageCachedLock) {
                if (fullImageCached == null) {
                    fullImageCached = Bitmap.createBitmap(ApiUtils.MAX_SIZE / 2, ApiUtils.MAX_SIZE / 2, Bitmap.Config.ARGB_8888);
                }
                fullImageCached.eraseColor(Color.TRANSPARENT);

                boolean useScaled = false;

                try {
                    Optimizer.BitmapInfo info = Optimizer.getInfo(fileTask.fileName);
                    useScaled = info.getWidth() > fullImageCached.getWidth() || info.getHeight() > fullImageCached.getHeight();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int scaledW = useScaled ? fileTask.getLocalPhoto().getFullW() / 2 : fileTask.getLocalPhoto().getFullW();
                int scaledH = useScaled ? fileTask.getLocalPhoto().getFullH() / 2 : fileTask.getLocalPhoto().getFullH();

                if (useScaled) {
                    BitmapDecoderEx.decodeReuseBitmapScaled(fileTask.fileName, fullImageCached);
                } else {
                    BitmapDecoderEx.decodeReuseBitmap(fileTask.fileName, fullImageCached);
                }

                Bitmap res = imageCache.findFree(SIZE_CHAT_PREVIEW);
                if (res == null) {
                    res = Bitmap.createBitmap(PREVIEW_MAX_W, PREVIEW_MAX_H, Bitmap.Config.ARGB_8888);
                } else {
                    res.eraseColor(Color.TRANSPARENT);
                }
                try {
                    int[] sizes = Optimizer.scaleToRatio(fullImageCached, scaledW, scaledH, res);

                    imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, fileTask.getKey(), sizes[0], sizes[1]),
                            MediaLoader.this);

                    notifyMediaLoaded(fileTask, res, sizes[0], sizes[1]);
                } finally {
                    // imageCache.putToCache(SIZE_CHAT_PREVIEW,res);
                }
            }
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaFileTask || task instanceof MediaVideoTask;
        }
    }

    private class MapWorker extends QueueWorker<BaseTask> {

        private DefaultHttpClient client;

        private MapWorker() {
            super(processor);
            HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
            HttpConnectionParams.setSoTimeout(httpParams, 5000);
            client = new DefaultHttpClient(httpParams);
            client.getParams().setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, false);
        }

        @Override
        protected boolean needRepeatOnError() {
            return true;
        }

        @Override
        protected boolean processTask(BaseTask task) throws Exception {
            if (!(task instanceof MediaGeoTask)) {
                return true;
            }

            MediaGeoTask geoTask = (MediaGeoTask) task;

            int scale = 1;
            float density = application.getResources().getDisplayMetrics().density;
            if (density >= 1.5f) {
                scale = 2;
            }

            String url = "https://maps.googleapis.com/maps/api/staticmap?" +
                    "center=" + geoTask.getLatitude() + "," + geoTask.getLongitude() +
                    "&zoom=15" +
                    "&size=" + (MAP_W / scale) + "x" + (MAP_H / scale) +
                    "&scale=" + scale +
                    "&sensor=false";

            HttpGet get = new HttpGet(url.replace(" ", "%20"));
            HttpResponse response = client.execute(get);
            if (response.getEntity().getContentLength() == 0) {
                throw new IOException();
            }

            if (response.getStatusLine().getStatusCode() == 404) {
                throw new IOException();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            response.getEntity().writeTo(outputStream);
            byte[] data = outputStream.toByteArray();

            Bitmap res = imageCache.findFree(SIZE_CHAT_PREVIEW);

            if (res != null) {
                Optimizer.loadTo(data, res);
            } else {
                res = Optimizer.load(data);
            }

            String cacheKey = "geo:" + geoTask.getLatitude() + "," + geoTask.getLongitude();
            imageCache.putToCache(SIZE_CHAT_PREVIEW, new BitmapHolder(res, cacheKey, MAP_W, MAP_H),
                    MediaLoader.this);
            notifyMediaLoaded(task, res, MAP_W, MAP_H);

            return true;
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaGeoTask;
        }
    }
}
