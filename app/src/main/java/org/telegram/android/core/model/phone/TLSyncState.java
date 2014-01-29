package org.telegram.android.core.model.phone;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 28.01.14.
 */
public class TLSyncState extends TLObject {

    public static final int CLASS_ID = 0xa31a8f57;

    private TLVector<TLSyncContact> contacts = new TLVector<TLSyncContact>();
    private TLVector<TLImportedPhone> importedPhones = new TLVector<TLImportedPhone>();

    public TLSyncState(List<TLSyncContact> contacts, List<TLImportedPhone> importedPhones) {
        this.contacts.addAll(contacts);
        this.importedPhones.addAll(importedPhones);
    }

    public TLSyncState() {
    }

    public TLVector<TLSyncContact> getContacts() {
        return contacts;
    }

    public TLVector<TLImportedPhone> getImportedPhones() {
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
