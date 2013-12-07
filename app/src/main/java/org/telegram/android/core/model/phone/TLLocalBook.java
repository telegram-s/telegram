package org.telegram.android.core.model.phone;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 07.12.13.
 */
public class TLLocalBook extends TLObject {
    public static final int CLASS_ID = 0x4f6371d4;

    private TLVector<TLLocalBookContact> contacts = new TLVector<TLLocalBookContact>();
    private TLVector<TLLocalImportedPhone> importedPhones = new TLVector<TLLocalImportedPhone>();

    public TLLocalBook() {

    }

    public TLVector<TLLocalBookContact> getContacts() {
        return contacts;
    }

    public TLVector<TLLocalImportedPhone> getImportedPhones() {
        return importedPhones;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLVector(contacts, stream);
        writeTLVector(importedPhones, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        contacts = readTLVector(stream, context);
        importedPhones = readTLVector(stream, context);
    }
}
