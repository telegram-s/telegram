package org.telegram.android.core;

import org.telegram.notifications.Notifications;

/**
 * Created by ex3ndr on 20.03.14.
 */
public class Events {
    public static final int KIND_AUDIO_RECORD = Notifications.KIND_START;
    public static final int KIND_AUDIO = KIND_AUDIO_RECORD + 1;

    public static final int STATE_STOP = 0;
    public static final int STATE_IN_PROGRESS = 1;
    public static final int STATE_ERROR = 2;
    public static final int STATE_PAUSED = 3;

    private Events() {

    }
}
