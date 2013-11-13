package org.telegram.android.core.model.service;

import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.10.13
 * Time: 2:04
 */
public class TLLocalActionUserEditPhoto extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x509e9d69;

    private TLLocalAvatarPhoto photo;

    public TLLocalActionUserEditPhoto() {

    }

    public TLLocalActionUserEditPhoto(TLLocalAvatarPhoto photo) {
        this.photo = photo;
    }

    public TLLocalAvatarPhoto getPhoto() {
        return photo;
    }

    public void setPhoto(TLLocalAvatarPhoto photo) {
        this.photo = photo;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(photo, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        photo = (TLLocalAvatarPhoto) readTLObject(stream, context);
    }
}
