package org.telegram.android.app;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import org.telegram.android.TelegramApplication;

public class NotificationRepeat extends IntentService {

    public NotificationRepeat() {
        super("NotificationRepeat");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ((TelegramApplication) getApplication()).getNotifications().renotify();
    }
}
