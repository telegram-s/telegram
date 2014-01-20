package org.telegram.android.gcm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.json.JSONObject;
import org.telegram.android.TelegramApplication;
import org.telegram.android.log.Logger;

/**
 * Created by ex3ndr on 14.01.14.
 */
public class GCMBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GCMBroadcastReceiver";

    public void onReceive(Context context, Intent intent) {
        Logger.d(TAG, "GCM Message received");

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);
        if (!intent.getExtras().isEmpty()) {
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
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

                        TelegramApplication application = ((TelegramApplication) context.getApplicationContext());
                        application.getKernel().getAuthKernel().getApiStorage().updateDCInfo(dc, ip, port);
                        application.getSyncKernel().getBackgroundSync().resetDcSync();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        setResultCode(Activity.RESULT_OK);
    }
}
