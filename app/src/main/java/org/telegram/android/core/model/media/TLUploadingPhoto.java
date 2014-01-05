package org.telegram.android.core.model.media;

import android.net.Uri;
import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Author: Korshakov Stepan
 * Created: 02.08.13 12:05
 */
public class TLUploadingPhoto extends TLObject {

    public static final int CLASS_ID = 0xf36d67f;

    private String fileName;
    private String fileUri;
    private int width;
    private int height;

    public TLUploadingPhoto() {

    }

    public TLUploadingPhoto(int width, int height, String fileName) {
        this.width = width;
        this.height = height;
        this.fileName = fileName;
        this.fileUri = "";
    }

    public TLUploadingPhoto(int width, int height, Uri uri) {
        this.width = width;
        this.height = height;
        this.fileName = "";
        this.fileUri = uri.toString();
    }

    public String getFileUri() {
        return fileUri;
    }

    public String getFileName() {
        return fileName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(fileName, stream);
        writeTLString(fileUri, stream);
        writeInt(width, stream);
        writeInt(height, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fileName = readTLString(stream);
        fileUri = readTLString(stream);
        width = readInt(stream);
        height = readInt(stream);
    }
}
