package org.telegram.android.core.model.phone;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 28.01.14.
 */
public class TLImportedPhone extends TLObject {

    public static final int CLASS_ID = 0x313fc34e;

    private String phone;
    private int uid;

    public TLImportedPhone(String phone, int uid) {
        this.phone = phone;
        this.uid = uid;
    }

    public TLImportedPhone() {
    }

    public String getPhone() {
        return phone;
    }

    public int getUid() {
        return uid;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(phone, stream);
        writeInt(uid, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        phone = readTLString(stream);
        uid = readInt(stream);
    }
}
