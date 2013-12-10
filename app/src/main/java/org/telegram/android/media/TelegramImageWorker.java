package org.telegram.android.media;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.workers.ImageWorker;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.upload.TLFile;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class TelegramImageWorker implements ImageWorker {

    private byte[] tempStorage = new byte[32 * 1024];

    private HashMap<Bitmap, Integer> imageReferences = new HashMap<Bitmap, Integer>();
    private HashMap<Integer, ArrayList<Bitmap>> imageCache = new HashMap<Integer, ArrayList<Bitmap>>();
    private HashMap<String, byte[]> imageDataCache = new HashMap<String, byte[]>();

    private StelsApplication application;

    public TelegramImageWorker(StelsApplication application) {
        this.application = application;
    }

    public void putToCache(Bitmap bmp) {
        if (bmp.getHeight() != bmp.getWidth()) {
            return;
        }

        ArrayList<Bitmap> sizeCache;
        if (!imageCache.containsKey(bmp.getHeight())) {
            sizeCache = new ArrayList<Bitmap>();
            imageCache.put(bmp.getHeight(), sizeCache);
        } else {
            sizeCache = imageCache.get(bmp.getHeight());
        }

        if (!sizeCache.contains(bmp)) {
            imageCache.get(bmp.getHeight()).add(bmp);
        }
    }

    private Bitmap fetchFromCache(int size) {
        if (imageCache.containsKey(size)) {
            ArrayList<Bitmap> cache = imageCache.get(size);
            if (cache.size() > 0) {
                Bitmap res = cache.get(0);
                cache.remove(0);
                return res;
            }
        }

        return null;
    }

    @Override
    public boolean acceptTask(ImageTask imageTask, ImageController imageController) {
        return imageTask instanceof TelegramImageTask;
    }

    @Override
    public int processTask(ImageTask imageTask, ImageController controller) {
        if (!application.isLoggedIn())
            return RESULT_FAILURE;

        try {
            TelegramImageTask stelsImageTask = (TelegramImageTask) imageTask;
            TLLocalFileLocation fileLocation = stelsImageTask.getFileLocation();

            byte[] data = null;

            if (imageDataCache.containsKey(imageTask.getKey())) {
                data = imageDataCache.get(imageTask.getKey());
            } else {
                TLFile res = application.getApi().doGetFile(fileLocation.getDcId(), new TLInputFileLocation(
                        fileLocation.getVolumeId(),
                        fileLocation.getLocalId(),
                        fileLocation.getSecret()),
                        0, 1024 * 1024 * 1024);
                data = res.getBytes();
            }


            if (data != null) {
                imageDataCache.put(imageTask.getKey(), data);

                Bitmap img;

                Bitmap cache = fetchFromCache(160);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                options.inSampleSize = 1;
                options.inTempStorage = tempStorage;

                if (cache != null) {
                    options.inBitmap = cache;
                }
                img = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                if (img == null) {
                    return RESULT_FAILURE;
                }

                imageTask.setResult(img);
                putToCache(img);

                imageTask.setBinaryResult(data);
                return RESULT_OK;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RESULT_FAILURE;
    }

    @Override
    public boolean isPausable() {
        return false;
    }
}
