package org.telegram.android.core.contacts;

import android.content.Context;
import org.telegram.android.core.model.phone.TLSyncState;
import org.telegram.android.critical.TLPersistence;
import org.telegram.tl.TLContext;

/**
 * Created by ex3ndr on 28.01.14.
 */
public class StatePersistence extends TLPersistence<TLSyncState> {

    public StatePersistence(Context context, String fileName, TLContext tlContext) {
        super(context, fileName, TLSyncState.class, tlContext);
    }
}
