package org.telegram.android;

import android.content.Context;
import android.content.Intent;

import com.google.android.gcm.GCMBaseIntentService;
import org.json.JSONObject;
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
        ((StelsApplication) getApplication()).getKernel().getSyncKernel().getBackgroundSync().onPushRegistered(regId);
    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onUnregistered(android.content.Context, java.lang.String)
     */
    @Override
    protected void onUnregistered(Context context, String regId) {

    }

    /**
     * @see com.google.android.gcm.GCMBaseIntentService#onMessage(android.content.Context, android.content.Intent)
     */
    @Override
    protected void onMessage(Context context, Intent intent) {
        try {
            String key = intent.getStringExtra("loc_key");
            if ("DC_UPDATE".equals(key)) {
                String data = intent.getStringExtra("custom");
                JSONObject object = new JSONObject(data);
                int dc = object.getInt("dc");
                String addr = object.getString("addr");
                String[] parts = addr.split(":");
                if (parts.length != 2) {
                    return;
                }
                String ip = parts[0];
                int port = Integer.parseInt(parts[1]);

                StelsApplication application = ((StelsApplication) context.getApplicationContext());
                application.getKernel().getAuthKernel().getApiStorage().updateDCInfo(dc, ip, port);
                application.getSyncKernel().getBackgroundSync().resetDcSync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ((StelsApplication) context.getApplicationContext()).getPushController().onPushArrived(intent);
    }


}