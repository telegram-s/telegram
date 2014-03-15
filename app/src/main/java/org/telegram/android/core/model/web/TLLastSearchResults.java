package org.telegram.android.core.model.web;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class TLLastSearchResults extends TLObject {
    public static final int CLASS_ID = 0x20b7cd45;

    private TLVector<TLSearchResult> lastResults = new TLVector<TLSearchResult>();

    public TLLastSearchResults() {
    }

    public TLVector<TLSearchResult> getLastResults() {
        return lastResults;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLVector(lastResults, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        lastResults = readTLVector(stream, context);
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
