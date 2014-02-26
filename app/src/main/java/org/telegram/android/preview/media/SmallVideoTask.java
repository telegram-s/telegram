package org.telegram.android.preview.media;

import org.telegram.android.core.model.media.TLLocalPhoto;
import org.telegram.android.core.model.media.TLLocalVideo;

/**
 * Created by ex3ndr on 26.02.14.
 */
public class SmallVideoTask extends BaseTask {
    private TLLocalVideo video;

    public SmallVideoTask(TLLocalVideo video) {
        this.video = video;
    }

    public TLLocalVideo getVideo() {
        return video;
    }

    @Override
    public String getKey() {
        return "s:" + video.getPreviewKey();
    }
}
