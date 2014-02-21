package org.telegram.android.preview;

import android.graphics.Bitmap;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.model.media.TLAbsLocalFileLocation;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.Optimizer;
import org.telegram.android.preview.cache.BitmapHolder;
import org.telegram.android.preview.queue.QueueProcessor;
import org.telegram.android.preview.queue.QueueWorker;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.upload.TLFile;

/**
 * Created by ex3ndr on 21.02.14.
 */
public class WallpaperLoader extends BaseLoader<WallpaperLoader.PreviewTask> {

    private static final int SIZE_SMALL = 0;

    public WallpaperLoader(TelegramApplication application) {
        super("wallpapers", 3, application);
    }

    public void requestPreview(TLAbsLocalFileLocation fileLocation, ImageReceiver receiver) {
        requestTask(new PreviewTask(fileLocation), receiver);
    }

    @Override
    protected QueueWorker<PreviewTask>[] createWorkers() {
        return new QueueWorker[]{
                new PreviewWorker()
        };
    }

    private Bitmap fetchPreviewBitmap() {
        Bitmap res = imageCache.findFree(SIZE_SMALL);
        if (res == null) {
            res = Bitmap.createBitmap(PreviewConfig.WALL_S_MAX_W, PreviewConfig.WALL_S_MAX_H, Bitmap.Config.ARGB_8888);
        }
        return res;
    }

    private class PreviewWorker extends QueueWorker<PreviewTask> {

        private Bitmap destBitmap = null;

        public PreviewWorker() {
            super(processor);
        }

        @Override
        protected boolean processTask(PreviewTask task) throws Exception {
            Bitmap dest = fetchPreviewBitmap();
            Optimizer.BitmapInfo cached = imageStorage.tryLoadFile(task.getKey() + "_preview", dest);
            if (cached != null) {
                onImageLoaded(dest, cached.getWidth(), cached.getHeight(), task, SIZE_SMALL);
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


            TLFile res;
            try {
                res = application.getApi().doGetFile(dcId, location, 0, 1024 * 1024 * 1024);
            } catch (Exception e) {
                imageCache.putFree(dest, SIZE_SMALL);
                throw e;
            }

            byte[] imgData = res.getBytes().cleanData();

            Optimizer.BitmapInfo info = Optimizer.getInfo(imgData);
            if (info.getWidth() <= PreviewConfig.WALL_D_MAX_W && info.getHeight() <= PreviewConfig.WALL_D_MAX_H && info.isJpeg()) {
                if (destBitmap == null) {
                    destBitmap = Bitmap.createBitmap(PreviewConfig.WALL_D_MAX_W, PreviewConfig.WALL_D_MAX_H, Bitmap.Config.ARGB_8888);
                }
                Optimizer.BitmapInfo info1 = Optimizer.loadTo(imgData, destBitmap);
                Optimizer.scaleToFill(destBitmap, info1.getWidth(), info1.getHeight(), dest);
            } else {
                Bitmap img = Optimizer.load(imgData);
                Optimizer.scaleToFill(img, img.getWidth(), img.getHeight(), dest);
            }

            putToDiskCache(task.getKey() + "_preview", dest, dest.getWidth(), dest.getHeight());
            onImageLoaded(dest, task, SIZE_SMALL);
            return true;
        }

        @Override
        public boolean isAccepted(PreviewTask task) {
            return true;
        }
    }

    public class PreviewTask extends QueueProcessor.BaseTask {

        private TLAbsLocalFileLocation fileLocation;

        public PreviewTask(TLAbsLocalFileLocation fileLocation) {
            this.fileLocation = fileLocation;
        }

        public TLAbsLocalFileLocation getFileLocation() {
            return fileLocation;
        }

        @Override
        public String getKey() {
            return fileLocation.getUniqKey();
        }
    }
}
