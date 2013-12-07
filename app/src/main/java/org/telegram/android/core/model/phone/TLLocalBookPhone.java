package org.telegram.android.core.model.phone;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class TLLocalBookPhone extends TLObject {

    public static final int CLASS_ID = 0x03abab38;

    private long id;
    private String phone;

    public TLLocalBookPhone(long id, String phone) {
        this.id = id;
        this.phone = phone;
    }

    public TLLocalBookPhone() {
    }

    public long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(id, stream);
        writeTLString(phone, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        id = readLong(stream);
        phone = readTLString(stream);
    }
}
