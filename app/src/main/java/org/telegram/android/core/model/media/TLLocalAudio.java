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
public class TLLocalAudio extends TLObject {

    public static final int CLASS_ID = 0x8739e5cb;

    private TLAbsLocalFileLocation fileLocation = new TLLocalFileEmpty();

    private int duration;

    public TLLocalAudio(TLAbsLocalFileLocation fileLocation, int duration) {
        this.fileLocation = fileLocation;
        this.duration = duration;
    }

    public TLLocalAudio() {

    }

    public int getDuration() {
        return duration;
    }

    public TLAbsLocalFileLocation getFileLocation() {
        return fileLocation;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(fileLocation, stream);
        writeInt(duration, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        fileLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        duration = readInt(stream);
    }
}
