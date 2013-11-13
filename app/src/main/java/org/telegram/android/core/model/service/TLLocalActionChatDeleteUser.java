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
 * Time: 19:38
 */
public class TLLocalActionChatDeleteUser extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x13f01b9e;

    protected int userId;

    public TLLocalActionChatDeleteUser(int userId) {
        this.userId = userId;
    }

    public TLLocalActionChatDeleteUser() {

    }

    public int getUserId() {
        return this.userId;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(userId, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        this.userId = readInt(stream);
    }
}
