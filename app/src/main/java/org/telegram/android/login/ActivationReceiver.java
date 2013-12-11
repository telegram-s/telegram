package org.telegram.android.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 8:35
 */
public class ActivationReceiver {

    private Context context;

    private BroadcastReceiver receiver;

    private ActivationListener listener;

    private int sentTime;

    private Thread checker = null;

    public ActivationReceiver(Context context) {
        this.context = context;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    for (int i = 0; i < pdus.length; i++) {
                        SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        if (msg == null)
                            continue;
                        if (msg.getDisplayOriginatingAddress() == null)
                            continue;

                        if (onSms(msg.getMessageBody())) {
                            abortBroadcast();
                        }
                    }
                }
            }
        };
    }

    private boolean onSms(String body) {
        try {
            Pattern p = Pattern.compile("[^0-9]*([0-9]+)[^0-9]*");
            Matcher m = p.matcher(body);
            if (m.find()) {
                foundedCode(Integer.parseInt(m.group(1)));
                return true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    private void foundedCode(int code) {
        if (listener != null) {
            listener.onCodeReceived(code);
            listener = null;
        }
    }

    public void startReceivingActivation(int sentTime, ActivationListener listener) {
        try {
            this.sentTime = sentTime;
            this.listener = listener;
            context.registerReceiver(receiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            if (this.checker != null) {
                this.checker.interrupt();
                this.checker = null;
            }
            this.checker = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    while (ActivationReceiver.this.listener != null && !isInterrupted()) {
                        check();
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            };
            this.checker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check() {
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"body", "address"},
                "date>?", new String[]{"" + (sentTime - 6) * 1000L}, "date desc limit 3");
        if (cursor.moveToFirst()) {
            do {
                String body = cursor.getString(0);
                onSms(body);
            } while (cursor.moveToNext());
        }
    }

    public void stopReceivingActivation() {
        try {
            if (this.checker != null) {
                this.checker.interrupt();
                this.checker = null;
            }
            this.listener = null;
            context.unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
