package org.telegram.android.core.model.storage;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 08.11.13
 * Time: 23:54
 */
public class TLDcInfo extends TLObject {

    public static final int CLASS_ID = 0x5d28839d;

    private int dcId;
    private String address;
    private int port;

    public TLDcInfo(int dcId, String address, int port) {
        this.dcId = dcId;
        this.address = address;
        this.port = port;
    }

    public TLDcInfo() {

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
