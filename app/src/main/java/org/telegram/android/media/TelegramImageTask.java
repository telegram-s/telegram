package org.telegram.android.media;

import com.extradea.framework.images.tasks.ImageTask;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.api.TLFileLocation;

/**
 * Created by ex3ndr on 10.12.13.
 */
public class TelegramImageTask extends ImageTask {
    private TLLocalFileLocation fileLocation;
    private int size;

    public TelegramImageTask(TLLocalFileLocation fileLocation, int size) {
        this.fileLocation = fileLocation;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public TLLocalFileLocation getFileLocation() {
        return fileLocation;
    }

    @Override
    public boolean skipDiskCacheCheck() {
        return true;
    }

    @Override
    protected String getKeyImpl() {
        return "t" + size + ":" + fileLocation.getDcId() + "+" + fileLocation.getVolumeId() + "+" + fileLocation.getLocalId();
    }
}
