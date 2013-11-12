package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 1:53
 */
public class TLUploadingVideo extends TLObject {

    public static final int CLASS_ID = 0xb0853a18;

    private String fileName;
    private int previewWidth;
    private int previewHeight;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public void setPreviewWidth(int previewWidth) {
        this.previewWidth = previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewHeight(int previewHeight) {
        this.previewHeight = previewHeight;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(fileName, stream);
        writeInt(previewWidth, stream);
        writeInt(previewHeight, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fileName = readTLString(stream);
        previewWidth = readInt(stream);
        previewHeight = readInt(stream);
    }
}
