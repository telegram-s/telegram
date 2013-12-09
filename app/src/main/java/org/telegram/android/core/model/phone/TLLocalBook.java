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
    public static final int CLASS_ID = 0x81374591;

    private TLVector<TLLocalImportedPhone> importedPhones = new TLVector<TLLocalImportedPhone>();

    public TLLocalBook() {

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
        writeTLVector(importedPhones, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        importedPhones = readTLVector(stream, context);
    }
}
