package org.telegram.android.core.model.service;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.10.13
 * Time: 17:30
 */
public class TLLocalActionEncryptedTtl extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x9c4203f2;

    private int ttlSeconds;

    public TLLocalActionEncryptedTtl(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public TLLocalActionEncryptedTtl() {

    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(ttlSeconds, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        ttlSeconds = readInt(stream);
    }
}
