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
    private boolean blur;
    private int blurRadius;

    public StelsImageTask(TLFileLocation fileLocation) {
        this.fileLocation = fileLocation;
    }

    public StelsImageTask(TLLocalFileLocation localFileLocation) {
        this.fileLocation = new TLFileLocation(localFileLocation.getDcId(), localFileLocation.getVolumeId(), localFileLocation.getLocalId(), localFileLocation.getSecret());
    }

    public void enableBlur(int r) {
        this.blurRadius = r;
        this.blur = true;
    }

    public boolean isBlur() {
        return blur;
    }

    public int getBlurRadius() {
        return blurRadius;
    }

    public TLFileLocation getFileLocation() {
        return fileLocation;
    }

    @Override
    protected String getKeyImpl() {
        return blur + ":" + blurRadius + ":" + fileLocation.getDcId() + "+" + fileLocation.getVolumeId() + "+" + fileLocation.getLocalId();
    }
}
