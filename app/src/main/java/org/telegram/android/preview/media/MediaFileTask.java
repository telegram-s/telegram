package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalPhoto;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class MediaFileTask extends BaseTask {

    private TLLocalPhoto localPhoto;
    private String fileName;

    public MediaFileTask(TLLocalPhoto localPhoto, String fileName, boolean isOut) {
        super(isOut);
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
    public String getStorageKey() {
        return fileName;
    }
}
