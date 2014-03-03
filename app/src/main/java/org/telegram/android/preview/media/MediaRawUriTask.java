package org.telegram.android.preview.media;

/**
 * Created by ex3ndr on 22.02.14.
 */
public class MediaRawUriTask extends BaseTask {
    private String uri;

    public MediaRawUriTask(String uri, boolean isOut) {
        super(isOut);
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String getStorageKey() {
        return uri;
    }
}
