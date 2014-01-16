package org.telegram.android.core.model.file;

/**
 * Created by ex3ndr on 16.01.14.
 */
public class FileUriSource extends AbsFileSource {
    private String uri;

    public FileUriSource(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }
}
