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
 * Time: 8:40
 */
public class TLLocalUserStatusOffline extends TLAbsLocalUserStatus {

    public static final int CLASS_ID = 0xf2c1e90e;

    protected int wasOnline;

    public TLLocalUserStatusOffline(int wasOnline) {
        this.wasOnline = wasOnline;
    }

    public TLLocalUserStatusOffline() {

    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    public int getWasOnline() {
        return wasOnline;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(wasOnline, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        wasOnline = readInt(stream);
    }
}
