package org.telegram.android.core.model.notifications;

import org.telegram.tl.TLContext;
import org.telegram.tl.TLObject;
import org.telegram.tl.TLVector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created by ex3ndr on 27.03.14.
 */
public class TLNotificationState extends TLObject {
    public static final int CLASS_ID = 0xc8284fe3;
    private TLVector<TLNotificationRecord> records = new TLVector<TLNotificationRecord>();
    private int unreadCount;

    public TLNotificationState() {
    }

    public TLVector<TLNotificationRecord> getRecords() {
        return records;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLVector(records, stream);
        writeInt(unreadCount, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        records = readTLVector(stream, context);
        unreadCount = readInt(stream);
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }
}
