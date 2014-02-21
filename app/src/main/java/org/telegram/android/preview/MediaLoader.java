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
import org.telegram.android.log.Logger;
import org.telegram.android.media.BitmapDecoderEx;
import org.telegram.android.media.Optimizer;
import org.telegram.android.media.VideoOptimizer;
import org.telegram.android.preview.cache.BitmapHolder;
import org.telegram.android.preview.cache.ImageCache;
import org.telegram.android.preview.cache.ImageStorage;
import org.telegram.android.preview.queue.QueueProcessor;
import org.telegram.android.preview.queue.QueueWorker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;

import static org.telegram.android.preview.PreviewConfig.*;

/**
 * Created by ex3ndr on 08.02.14.
 */
public class MediaLoader extends BaseLoader<MediaLoader.BaseTask> {

    private static final int SIZE_CHAT_PREVIEW = 0;
    private static final int SIZE_MAP_PREVIEW = 1;

    private Bitmap fullImageCached = null;
    private final Object fullImageCachedLock = new Object();

    private ThreadLocal<Bitmap> fastBitmaps = new ThreadLocal<Bitmap>() {
        @Override
        protected Bitmap initialValue() {
            return Bitmap.createBitmap(FAST_MAX_W, FAST_MAX_H, Bitmap.Config.ARGB_8888);
        }
    };

    public MediaLoader(TelegramApplication application) {
        super("previews", 5, application);
    }

    @Override
    protected QueueWorker<BaseTask>[] createWorkers() {
        return new QueueWorker[]{
                new FastWorker(),
                new FileWorker(),
                new MapWorker(),
                new RawWorker()
        };
    }

    public void requestRaw(String fileName, ImageReceiver receiver) {
        requestTask(new MediaRawTask(fileName), receiver);
    }

    public void requestRawUri(String fileName, ImageReceiver receiver) {
        requestTask(new MediaRawUriTask(fileName), receiver);
    }

    public void requestGeo(TLLocalGeo geo, ImageReceiver receiver) {
        requestTask(new MediaGeoTask(geo.getLatitude(), geo.getLongitude()), receiver);
    }

    public void requestFastLoading(TLLocalPhoto photo, ImageReceiver receiver) {
        requestTask(new MediaPhotoFastTask(photo), receiver);
    }

    public void requestFastLoading(TLLocalDocument doc, ImageReceiver receiver) {
        requestTask(new MediaDocFastTask(doc), receiver);
    }

    public void requestFastLoading(TLLocalVideo video, ImageReceiver receiver) {
        requestTask(new MediaVideoFastTask(video), receiver);
    }

    public void requestFullLoading(TLLocalPhoto photo, String fileName, ImageReceiver receiver) {
        requestTask(new MediaFileTask(photo, fileName), receiver);
    }

    public void requestVideoLoading(String fileName, ImageReceiver receiver) {
        requestTask(new MediaVideoTask(fileName), receiver);
    }

    public void cancelRequest(ImageReceiver receiver) {
        super.cancelRequest(receiver);
    }

    private Bitmap fetchChatPreview() {
        Bitmap res = imageCache.findFree(SIZE_CHAT_PREVIEW);
        if (res == null) {
            res = Bitmap.createBitmap(PreviewConfig.MAX_PREVIEW_BITMAP_W, PreviewConfig.MAX_PREVIEW_BITMAP_H, Bitmap.Config.ARGB_8888);
        } else {
            res.eraseColor(Color.TRANSPARENT);
        }
        return res;
    }

    private Bitmap fetchMapPreview() {
        Bitmap res = imageCache.findFree(SIZE_MAP_PREVIEW);
        if (res == null) {
            res = Bitmap.createBitmap(PreviewConfig.MAP_W, PreviewConfig.MAP_H, Bitmap.Config.ARGB_8888);
        } else {
            res.eraseColor(Color.TRANSPARENT);
        }
        return res;
    }

    private int[] drawPreview(Bitmap src, int w, int h, Bitmap dest) {
        return Optimizer.scaleToRatioRounded(src, w, h,
                dest, PreviewConfig.MIN_PREVIEW_W, PreviewConfig.MIN_PREVIEW_H,
                PreviewConfig.ROUND_RADIUS);
    }

    public abstract class BaseTask extends QueueProcessor.BaseTask {

    }

    public class MediaFileTask extends BaseTask {

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

    public class MediaGeoTask extends BaseTask {
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

    public class MediaPhotoFastTask extends BaseTask {

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

    public class MediaDocFastTask extends BaseTask {

        private TLLocalDocument doc;

        private MediaDocFastTask(TLLocalDocument doc) {
            this.doc = doc;
        }

        public TLLocalDocument getDoc() {
            return doc;
        }

        @Override
        public String getKey() {
            // Work-around: documents with fast cache doesn't contain preview location
            return "preview:" + doc.getFileLocation().getUniqKey();
        }
    }

    public class MediaVideoFastTask extends BaseTask {

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

    public class MediaVideoTask extends BaseTask {
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

    private class MediaRawUriTask extends BaseTask {
        private String uri;

        private MediaRawUriTask(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        @Override
        public String getKey() {
            return uri;
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
            Bitmap img = fastBitmaps.get();
            try {
                Optimizer.loadTo(data, img);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            //img = BitmapUtils.fastblur(img, w, h, 8);
            Optimizer.blur(img);

            Bitmap destPreview = fetchChatPreview();


            int[] sizes = drawPreview(img, w, h, destPreview);

            onImageLoaded(destPreview, sizes[0], sizes[1], task, SIZE_CHAT_PREVIEW);

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
            } else if (task instanceof MediaRawUriTask) {
                processTask((MediaRawUriTask) task);
            }
            return true;
        }

        private void processTask(MediaRawUriTask rawTask) throws Exception {
            Bitmap res = fetchChatPreview();
            Optimizer.BitmapInfo info = tryToLoadFromCache(rawTask.getKey(), res);
            if (info != null) {
                onImageLoaded(res, info.getWidth(), info.getHeight(), rawTask, SIZE_CHAT_PREVIEW);
                return;
            }
            Bitmap tmp = Optimizer.optimize(rawTask.getUri(), application);
            int[] sizes = drawPreview(tmp, tmp.getWidth(), tmp.getHeight(), res);
            putToDiskCache(rawTask.getKey(), res, sizes[0], sizes[1]);

            onImageLoaded(tmp, sizes[0], sizes[1], rawTask, SIZE_CHAT_PREVIEW);
        }

        private void processTask(MediaRawTask rawTask) throws Exception {
            synchronized (fullImageCachedLock) {
                if (fullImageCached == null) {
                    fullImageCached = Bitmap.createBitmap(ApiUtils.MAX_SIZE / 2, ApiUtils.MAX_SIZE / 2, Bitmap.Config.ARGB_8888);
                }
            }

            Bitmap res = fetchChatPreview();

            Optimizer.BitmapInfo info = tryToLoadFromCache(rawTask.getKey(), res);
            if (info != null) {
                onImageLoaded(res, info.getWidth(), info.getHeight(), rawTask, SIZE_CHAT_PREVIEW);
                return;
            }

            info = Optimizer.getInfo(rawTask.fileName);

            if (info.getMimeType() != null && info.getMimeType().equals("image/jpeg")) {
                if (info.getHeight() <= fullImageCached.getHeight() && info.getWidth() <= fullImageCached.getWidth()) {
                    synchronized (fullImageCachedLock) {
                        BitmapDecoderEx.decodeReuseBitmap(rawTask.fileName, fullImageCached);
                        int[] sizes = Optimizer.scaleToRatio(fullImageCached, info.getWidth(), info.getHeight(), res);
                        onImageLoaded(res, sizes[0], sizes[1], rawTask, SIZE_CHAT_PREVIEW);
                        return;
                    }
                } else if (info.getWidth() / 2 <= fullImageCached.getWidth() && info.getHeight() / 2 <= fullImageCached.getHeight()) {
                    synchronized (fullImageCachedLock) {
                        BitmapDecoderEx.decodeReuseBitmap(rawTask.fileName, fullImageCached);
                        int[] sizes = Optimizer.scaleToRatio(fullImageCached, info.getWidth() / 2, info.getHeight() / 2, res);
                        onImageLoaded(res, sizes[0], sizes[1], rawTask, SIZE_CHAT_PREVIEW);
                        return;
                    }
                }
            }

            Bitmap tmp = Optimizer.optimize(rawTask.getFileName());
            int[] sizes = Optimizer.scaleToRatio(tmp, tmp.getWidth(), tmp.getHeight(), res);
            putToDiskCache(rawTask.getKey(), res, sizes[0], sizes[1]);
            onImageLoaded(res, sizes[0], sizes[1], rawTask, SIZE_CHAT_PREVIEW);
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
            Bitmap res = fetchChatPreview();
            Optimizer.BitmapInfo info = tryToLoadFromCache(task.getKey(), res);
            if (info != null) {
                onImageLoaded(res, info.getWidth(), info.getHeight(), task, SIZE_CHAT_PREVIEW);
                return;
            }

            VideoOptimizer.VideoMetadata metadata = VideoOptimizer.getVideoSize(task.getFileName());
            int[] sizes = drawPreview(metadata.getImg(), metadata.getImg().getWidth(), metadata.getImg().getHeight(), res);
            putToDiskCache(task.getKey(), res, sizes[0], sizes[1]);

            onImageLoaded(res, sizes[0], sizes[1], task, SIZE_CHAT_PREVIEW);
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

                Bitmap res = fetchChatPreview();
                int[] sizes = drawPreview(fullImageCached, scaledW, scaledH, res);

                onImageLoaded(res, sizes[0], sizes[1], fileTask, SIZE_CHAT_PREVIEW);
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

            Bitmap res = fetchMapPreview();
            Optimizer.BitmapInfo info = tryToLoadFromCache(geoTask.getKey(), res);
            if (info != null) {
                onImageLoaded(res, info.getWidth(), info.getHeight(), task, SIZE_MAP_PREVIEW);
                return true;
            }

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
                    "&sensor=false" +
                    "&format=jpg";

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

            Optimizer.loadTo(data, res);

            putToDiskCache(task.getKey(), res, MAP_W, MAP_H);

            onImageLoaded(res, MAP_W, MAP_H, task, SIZE_MAP_PREVIEW);
            return true;
        }

        @Override
        public boolean isAccepted(BaseTask task) {
            return task instanceof MediaGeoTask;
        }
    }
}
