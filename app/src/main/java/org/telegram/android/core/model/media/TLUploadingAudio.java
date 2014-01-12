package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 12.01.14.
 */
public class TLUploadingAudio extends TLObject {
    public static final int CLASS_ID = 0xd3f95c3b;

    private String fileName;
    private int duration;

    public TLUploadingAudio(String fileName, int duration) {
        this.fileName = fileName;
        this.duration = duration;
    }

    public TLUploadingAudio() {

    }

    public String getFileName() {
        return fileName;
    }

    public int getDuration() {
        return duration;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(fileName, stream);
        writeInt(duration, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fileName = readTLString(stream);
        duration = readInt(stream);
    }
}
