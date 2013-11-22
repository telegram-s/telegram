package org.telegram.android.kernel.compat;

import org.telegram.android.core.model.TLLocalContext;
import org.telegram.mtproto.log.Logger;
import org.telegram.tl.TLObject;

import java.io.IOException;

/**
 * Created by ex3ndr on 22.11.13.
 */
public class CompatDeserializer {

    private static final String TAG = "CompatDeserializer";

    public static TLObject deserialize(byte[] data) {
        if (data.length < 4) {
            return null;
        }
        if (data[0] == 0xac && data[1] == 0xed) {
            Logger.d(TAG, "Founded java object");
            return null;
        }
        try {
            return TLLocalContext.getInstance().deserializeMessage(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
