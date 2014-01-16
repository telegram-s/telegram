package org.telegram.android.media;

import android.graphics.Bitmap;
import com.extradea.framework.images.ImageController;
import com.extradea.framework.images.tasks.ImageTask;
import com.extradea.framework.images.workers.ImageWorker;
import org.telegram.android.StelsApplication;
import org.telegram.android.ui.BitmapUtils;
import org.telegram.api.TLFileLocation;
import org.telegram.api.TLInputFileLocation;
import org.telegram.api.upload.TLFile;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 6:55
 */
public class StelsImageWorker implements ImageWorker {

    private StelsApplication application;

    public StelsImageWorker(StelsApplication application) {
        this.application = application;
    }

    @Override
    public boolean acceptTask(ImageTask task, ImageController controller) {
        return task instanceof StelsImageTask;
    }

    @Override
    public int processTask(ImageTask task, ImageController controller) {
        if (!application.isLoggedIn())
            return RESULT_FAILURE;

        try {
            StelsImageTask stelsImageTask = (StelsImageTask) task;
            TLFileLocation fileLocation = stelsImageTask.getFileLocation();

            TLFile res = application.getApi().doGetFile(fileLocation.getDcId(), new TLInputFileLocation(
                    fileLocation.getVolumeId(),
                    fileLocation.getLocalId(),
                    fileLocation.getSecret()),
                    0, 1024 * 1024 * 1024);

            if (res != null) {
                Bitmap img = controller.getBitmapDecoder().decodeBitmap(res.getBytes());
                if (stelsImageTask.isBlur()) {
                    img = BitmapUtils.fastblur(img, stelsImageTask.getBlurRadius());
                }
                task.setResult(img);
                task.setBinaryResult(res.getBytes());
                return RESULT_OK;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RESULT_FAILURE;
    }

    @Override
    public boolean isPausable() {
        return true;
    }
}
