package org.telegram.android.core.model.service;

import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:58
 */
public class TLLocalActionChatEditTitle extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x518b8515;

    private String title;

    public TLLocalActionChatEditTitle(String title) {
        this.title = title;
    }

    public TLLocalActionChatEditTitle() {

    }

    public String getTitle() {
        return title;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLString(title, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        title = readTLString(stream);
    }
}
