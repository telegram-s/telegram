package org.telegram.android.kernel.compat.v5;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;
import static org.telegram.tl.StreamingUtils.readInt;

/**
 * Created by ex3ndr on 03.12.13.
 */
public class TLDcInfoCompat extends TLObject {
    public static final int CLASS_ID = 0x5d28839d;

    private int dcId;
    private String address;
    private int port;

    public TLDcInfoCompat(int dcId, String address, int port) {
        this.dcId = dcId;
        this.address = address;
        this.port = port;
    }

    public TLDcInfoCompat() {

    }

    public int getDcId() {
        return dcId;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(dcId, stream);
        writeTLString(address, stream);
        writeInt(port, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        dcId = readInt(stream);
        address = readTLString(stream);
        port = readInt(stream);
    }
}
