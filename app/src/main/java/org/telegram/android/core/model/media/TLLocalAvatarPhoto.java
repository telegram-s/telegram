package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 20:22
 */
public class TLLocalAvatarPhoto extends TLAbsLocalAvatarPhoto {

    public static final int CLASS_ID = 0x880e9649;


    private TLAbsLocalFileLocation previewLocation;
    private TLAbsLocalFileLocation fullLocation;

    public TLLocalAvatarPhoto() {

    }

    public TLAbsLocalFileLocation getPreviewLocation() {
        return previewLocation;
    }

    public void setPreviewLocation(TLAbsLocalFileLocation previewLocation) {
        this.previewLocation = previewLocation;
    }

    public TLAbsLocalFileLocation getFullLocation() {
        return fullLocation;
    }

    public void setFullLocation(TLAbsLocalFileLocation fullLocation) {
        this.fullLocation = fullLocation;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(previewLocation, stream);
        writeTLObject(fullLocation, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        previewLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
        fullLocation = (TLAbsLocalFileLocation) readTLObject(stream, context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TLLocalAvatarPhoto)) {
            return false;
        }
        return equals((TLLocalAvatarPhoto) o);
    }

    public boolean equals(TLLocalAvatarPhoto photo) {
        return true;
    }
}