package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalPhoto;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallPhotoTask extends BaseTask {
    private TLLocalPhoto photo;

    public SmallPhotoTask(TLLocalPhoto photo) {
        super(true);
        this.photo = photo;
    }

    public TLLocalPhoto getPhoto() {
        return photo;
    }

    @Override
    public String getStorageKey() {
        return "s:" + photo.getFastPreviewKey();
    }
}
