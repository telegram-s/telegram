package org.telegram.android.core.model.media;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 22:44
 */
public class TLLocalUnknown extends TLObject {

    public static final int CLASS_ID = 0xdef0776e;

    private byte[] data;

    public TLLocalUnknown(byte[] data) {
        this.data = data;
    }

    public TLLocalUnknown() {

    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLBytes(data, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        data = readTLBytes(stream);
    }
}
