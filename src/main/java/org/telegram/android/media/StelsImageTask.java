package org.telegram.android.media;

import com.extradea.framework.images.tasks.ImageTask;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.api.TLFileLocation;

/**
 * Author: Korshakov Stepan
 * Created: 01.08.13 6:54
 */
public class StelsImageTask extends ImageTask {
    private TLFileLocation fileLocation;

    public StelsImageTask(TLFileLocation fileLocation) {
        this.fileLocation = fileLocation;
    }

    public StelsImageTask(TLLocalFileLocation localFileLocation) {
        this.fileLocation = new TLFileLocation(localFileLocation.getDcId(), localFileLocation.getVolumeId(), localFileLocation.getLocalId(), localFileLocation.getSecret());
    }

    public TLFileLocation getFileLocation() {
        return fileLocation;
    }

    @Override
    protected String getKeyImpl() {
        return getMaxWidth() + ":" + getMaxHeight() + ":" + isFillRect() + ":" + fileLocation.getDcId() + "+" + fileLocation.getVolumeId() + "+" + fileLocation.getLocalId();
    }
}
