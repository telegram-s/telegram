package org.telegram.android.core.audio;

import android.widget.Toast;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorReference;
import org.telegram.threading.ActorSystem;
import org.telegram.threading.ReflectedActor;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AudioPlayerActor extends ReflectedActor {

    public static final String SUB_START = "subStart";
    public static final String SUB_CRASH = "subCrash";
    public static final String SUB_IN_PROGRESS = "subInProgress";
    public static final String SUB_IN_PAUSED = "subPause";
    public static final String SUB_STOP = "subStop";

    private ActorReference androidPlayerActor;
    private ActorReference opusPlayerActor;

    private OpusLib opusLib;

    private boolean isInited;

    private boolean usedAndroid;
    private long currentId;
    private TelegramApplication application;

    public AudioPlayerActor(TelegramApplication application, ActorSystem system) {
        super(system, "common");
        this.application = application;
        androidPlayerActor = new AndroidPlayerActor(self(), application, system).self();
        opusPlayerActor = new OpusPlayerActor(self(), system).self();
        opusLib = new OpusLib();
    }

    protected void onPlayMessage(long id, String fileName) {
        if (isInited) {
            onStopMessage();
        }

        this.usedAndroid = opusLib.isOpusFile(fileName) <= 0;
        this.currentId = id;
        this.isInited = true;

        if (usedAndroid) {
            androidPlayerActor.talk("play", self(), currentId, fileName);
            opusPlayerActor.talk("stop", self());
        } else {
            opusPlayerActor.talk("play", self(), currentId, fileName);
            androidPlayerActor.talk("stop", self());
        }
        flushNotifications();
    }

    protected void onStopMessage() {
        if (isInited) {
            if (usedAndroid) {
                androidPlayerActor.talk("stop", self());
            } else {
                opusPlayerActor.talk("stop", self());
            }
            notifyPending(Events.STATE_STOP);
        }

        currentId = -1;
        isInited = false;
    }

    protected void onToggleMessage(long id, String fileName) {
        if (isInited) {
            if (id == currentId) {
                if (usedAndroid) {
                    androidPlayerActor.talk("toggle", self(), id, fileName);
                } else {
                    opusPlayerActor.talk("toggle", self(), id, fileName);
                }
            } else {
                onStopMessage();
            }
        }

        if (!isInited) {
            onPlayMessage(id, fileName);
        }
    }

    protected void onSubStartMessage(long id) {
        if (id != currentId || !isInited) {
            return;
        }
        notify(Events.STATE_IN_PROGRESS, 0.0f);
    }

    protected void onSubStopMessage(long id) {
        if (id != currentId || !isInited) {
            return;
        }
        notify(Events.STATE_STOP);
    }

    protected void onSubPauseMessage(long id, float progress) {
        if (id != currentId || !isInited) {
            return;
        }
        notify(Events.STATE_PAUSED, progress);
    }

    protected void onSubInProgressMessage(long id, float progress) {
        if (id != currentId || !isInited) {
            return;
        }
        notifyUpdate(Events.STATE_IN_PROGRESS, progress);
    }

    protected void onSubCrashMessage(long id) {
        if (id != currentId || !isInited) {
            return;
        }

        Toast.makeText(application, "File error", Toast.LENGTH_SHORT).show();
        notify(Events.STATE_STOP);
    }

    private void notifyUpdate(int state, Object... args) {
        if (isInited) {
            application.getUiKernel().getUiNotifications().sendStateUpdate(Events.KIND_AUDIO, currentId, state, args);
        }
    }

    private void notify(int state, Object... args) {
        if (isInited) {
            application.getUiKernel().getUiNotifications().sendState(Events.KIND_AUDIO, currentId, state, args);
        }
    }

    private void notifyPending(int state, Object... args) {
        if (isInited) {
            application.getUiKernel().getUiNotifications().sendDelayedState(Events.KIND_AUDIO, currentId, state, args);
        }
    }

    private void flushNotifications() {
        application.getUiKernel().getUiNotifications().flushPending();
    }
}