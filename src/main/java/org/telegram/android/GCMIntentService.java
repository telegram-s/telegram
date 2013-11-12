package org.telegram.android;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;
import org.telegram.android.R;


public class GCMIntentService extends GCMBaseIntentService {
    public GCMIntentService() {
        super("Test");
    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onError(android.content.Context, java.lang.String)
     */
    @Override
    protected void onError(Context arg0, String error) {
    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onRegistered(android.content.Context, java.lang.String)
     */
    @Override
    protected void onRegistered(Context context, final String regId) {
        ((StelsApplication) getApplication()).getTechSyncer().registerPush(regId);
    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onUnregistered(android.content.Context, java.lang.String)
     */
    @Override
    protected void onUnregistered(Context context, String regId) {
        ((StelsApplication) getApplication()).getTechSyncer().unregisterPush(regId);
    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onMessage(android.content.Context, android.content.Intent)
     */
    @Override
    protected void onMessage(Context context, Intent intent) {
        // ((StelsApplication) context.getApplicationContext()).getPushController().onPushArrived(intent);
    }


}