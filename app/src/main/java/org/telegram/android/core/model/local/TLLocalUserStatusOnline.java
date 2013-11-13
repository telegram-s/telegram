package org.telegram.android.core.model.local;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 8:24
 */
public class TLLocalUserStatusOnline extends TLAbsLocalUserStatus {

    public static final int CLASS_ID = 0xfd34c67c;

    protected int expires;

    public TLLocalUserStatusOnline(int expires) {
        this.expires = expires;
    }

    public TLLocalUserStatusOnline() {

    }

    public int getExpires() {
        return expires;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(expires, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        expires = readInt(stream);
    }
}
