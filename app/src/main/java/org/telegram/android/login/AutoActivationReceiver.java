package org.telegram.android.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import org.telegram.android.log.Logger;
import org.telegram.android.reflection.CrashHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 8:35
 */
public class AutoActivationReceiver {

    private static final String TAG = "AutoActivationReceiver";

    private Context context;

    private BroadcastReceiver receiver;

    private AutoActivationListener listener;

    private int sentTime;

    private Thread checker = null;

    private Handler handler = new Handler(Looper.getMainLooper());

    public AutoActivationReceiver(Context context) {
        this.context = context;
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        if (pdus != null) {
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
                } catch (Exception e) {
                    Logger.t(TAG, e);
                    CrashHandler.logHandledException(e);
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
        } catch (Exception e) {
            Logger.t(TAG, e);
            CrashHandler.logHandledException(e);
        }
        return false;
    }

    private void foundedCode(final int code) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Logger.d(TAG, "notify");
                if (listener != null) {
                    listener.onCodeReceived(code);
                    listener = null;
                }
            }
        });
    }

    public void startReceivingActivation(int sentTime, AutoActivationListener listener) {
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
                    while (AutoActivationReceiver.this.listener != null && !isInterrupted()) {
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
            Logger.t(TAG, e);
            CrashHandler.logHandledException(e);
        }
    }

    private void check() {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"body", "address"},
                    "date>?", new String[]{"" + (sentTime - 6) * 1000L}, "date desc limit 3");
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String body = cursor.getString(0);
                        onSms(body);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Logger.t(TAG, e);
            CrashHandler.logHandledException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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
