package org.telegram.android.core.model.local;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 15.01.14.
 */
public class TLLocalFullChatInfo extends TLObject {

    public static final int CLASS_ID = 0x8af98247;

    private int adminId;
    private TLVector<TLLocalChatParticipant> users = new TLVector<TLLocalChatParticipant>();
    private boolean isForbidden;

    public TLLocalFullChatInfo(int adminId, List<TLLocalChatParticipant> users) {
        this.adminId = adminId;
        this.users.addAll(users);
    }

    public TLLocalFullChatInfo() {

    }

    public int getAdminId() {
        return adminId;
    }

    public TLVector<TLLocalChatParticipant> getUsers() {
        return users;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public void setUsers(TLVector<TLLocalChatParticipant> users) {
        this.users = users;
    }

    public boolean isForbidden() {
        return isForbidden;
    }

    public void setForbidden(boolean isForbidden) {
        this.isForbidden = isForbidden;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(adminId, stream);
        writeTLVector(users, stream);
        writeTLBool(isForbidden, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        adminId = readInt(stream);
        users = readTLVector(stream, context);
        isForbidden = readTLBool(stream);
    }
}
