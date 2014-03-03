package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.preview.media.BaseTask;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class MediaPhotoFastTask extends BaseTask {

    private TLLocalPhoto photo;

    public MediaPhotoFastTask(TLLocalPhoto photo, boolean isOut) {
        super(isOut);
        this.photo = photo;
    }

    public TLLocalPhoto getPhoto() {
        return photo;
    }

    @Override
    public String getStorageKey() {
        return photo.getFastPreviewKey();
    }
}
