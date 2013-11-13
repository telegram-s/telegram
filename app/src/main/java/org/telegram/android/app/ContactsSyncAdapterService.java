package org.telegram.android.app;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.telegram.android.log.Logger;

/**
 * Author: Korshakov Stepan
 * Created: 13.08.13 21:23
 */
public class ContactsSyncAdapterService extends Service {
    private static final String TAG = "ContactsSyncAdapter";
    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    public ContactsSyncAdapterService() {
        super();
    }

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            try {
                ContactsSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult);
            } catch (OperationCanceledException e) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new SyncAdapterImpl(this);
        return sSyncAdapter;
    }

    private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
        mContentResolver = context.getContentResolver();
        Logger.d(TAG, "performSync: " + account.toString());
        //This is where the magic will happen!
    }

}