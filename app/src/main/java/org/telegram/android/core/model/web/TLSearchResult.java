package org.telegram.android.core.model.web;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 15.03.14.
 */
public class TLSearchResult extends TLObject {

    public static final int CLASS_ID = 0x150120b9;

    private String key;
    private int size;
    private int w;
    private int h;
    private String fullUrl;
    private int thumbW;
    private int thumbH;
    private String thumbUrl;
    private String contentType;

    public TLSearchResult(String key, int size, int w, int h, String fullUrl, int thumbW, int thumbH, String thumbUrl, String contentType) {
        this.key = key;
        this.size = size;
        this.w = w;
        this.h = h;
        this.fullUrl = fullUrl;
        this.thumbW = thumbW;
        this.thumbH = thumbH;
        this.thumbUrl = thumbUrl;
        this.contentType = contentType;
    }

    public TLSearchResult() {
    }

    public String getKey() {
        return key;
    }

    public int getSize() {
        return size;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public int getThumbW() {
        return thumbW;
    }

    public int getThumbH() {
        return thumbH;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(key, stream);
        writeTLString(contentType, stream);
        writeInt(size, stream);
        writeInt(w, stream);
        writeInt(h, stream);
        writeTLString(fullUrl, stream);
        writeInt(thumbW, stream);
        writeInt(thumbH, stream);
        writeTLString(thumbUrl, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        key = readTLString(stream);
        contentType = readTLString(stream);
        size = readInt(stream);
        w = readInt(stream);
        h = readInt(stream);
        fullUrl = readTLString(stream);
        thumbW = readInt(stream);
        thumbH = readInt(stream);
        thumbUrl = readTLString(stream);
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
