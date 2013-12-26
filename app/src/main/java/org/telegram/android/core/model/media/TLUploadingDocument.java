package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 14.12.13.
 */
public class TLUploadingDocument extends TLObject {

    public static final int CLASS_ID = 0x45dfcbb9;

    private String fileName;
    private int fileSize;

    public TLUploadingDocument(String fileName, int fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public TLUploadingDocument() {
    }

    public int getFileSize() {
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(fileName, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fileName = readTLString(stream);
    }
}
