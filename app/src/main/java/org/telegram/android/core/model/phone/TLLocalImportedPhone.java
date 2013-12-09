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
public class TLLocalImportedPhone extends TLObject {

    public static final int CLASS_ID = 0xdb1e4a4b;

    private String phone;
    private int uid;
    private boolean isImported;

    public TLLocalImportedPhone(String phone, int uid, boolean isImported) {
        this.phone = phone;
        this.uid = uid;
        this.isImported = isImported;
    }

    public TLLocalImportedPhone() {
    }

    public String getPhone() {
        return phone;
    }

    public int getUid() {
        return uid;
    }

    public boolean isImported() {
        return isImported;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(phone, stream);
        writeInt(uid, stream);
        writeTLBool(isImported, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        phone = readTLString(stream);
        uid = readInt(stream);
        isImported = readTLBool(stream);
    }
}
