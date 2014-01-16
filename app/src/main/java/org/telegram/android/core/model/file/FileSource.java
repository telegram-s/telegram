package org.telegram.android.core.model.file;

/**
 * Created by ex3ndr on 16.01.14.
 */
public class FileSource extends AbsFileSource {
    private String fileName;

    public FileSource(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
