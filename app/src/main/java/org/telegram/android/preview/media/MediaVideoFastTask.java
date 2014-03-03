package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalVideo;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class MediaVideoFastTask extends BaseTask {

    private TLLocalVideo video;

    public MediaVideoFastTask(TLLocalVideo video, boolean isOut) {
        super(isOut);
        this.video = video;
    }

    public TLLocalVideo getVideo() {
        return video;
    }

    @Override
    public String getStorageKey() {
        return video.getPreviewKey();
    }
}
