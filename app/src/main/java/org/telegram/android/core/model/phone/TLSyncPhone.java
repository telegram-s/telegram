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
public class TLSyncPhone extends TLObject {

    public static final int CLASS_ID = 0x0972f6d4;

    private long phoneId;
    private String number;
    private boolean isPrimary;
    private boolean isImported;

    public TLSyncPhone(long phoneId, String number, boolean isPrimary, boolean isImported) {
        this.phoneId = phoneId;
        this.number = number;
        this.isPrimary = isPrimary;
        this.isImported = isImported;
    }

    public TLSyncPhone() {
    }

    public long getPhoneId() {
        return phoneId;
    }

    public String getNumber() {
        return number;
    }

    public boolean isPrimary() {
        return isPrimary;
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
        writeLong(phoneId, stream);
        writeTLString(number, stream);
        writeTLBool(isPrimary, stream);
        writeTLBool(isImported, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        phoneId = readLong(stream);
        number = readTLString(stream);
        isPrimary = readTLBool(stream);
        isImported = readTLBool(stream);
    }
}
