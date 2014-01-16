package org.telegram.android.media;

import android.graphics.*;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.workers.ImageWorker;
import org.telegram.android.ui.BitmapUtils;

/**
 * Author: Korshakov Stepan
 * Created: 20.08.13 1:14
 */
public class CachedImageWorker implements ImageWorker {

    public CachedImageWorker() {
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof CachedImageTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        CachedImageTask cachedImageTask = (CachedImageTask) task;
        byte[] image = cachedImageTask.getData();
        Bitmap src = BitmapFactory.decodeByteArray(image, 0, image.length);

        if (cachedImageTask.isBlur()) {
            Bitmap filtered = BitmapUtils.fastblur(src, 10);
            src.recycle();
            src = filtered;
        }

        task.setResult(src);
        return RESULT_OK;
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}