package org.telegram.android.core.audio;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class VoiceRecorder {
    public static final int STATE_NONE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_IN_PROGRESS = 2;
    public static final int STATE_STOPPING = 3;
    private int state = STATE_NONE;
}
