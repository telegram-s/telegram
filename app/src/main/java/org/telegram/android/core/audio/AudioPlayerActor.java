package org.telegram.android.core.audio;

import android.widget.Toast;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
import org.telegram.opus.OpusLib;
import org.telegram.actors.*;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AudioPlayerActor extends ReflectedActor {

    private ClientMessenger androidPlayerActor;
    private ClientMessenger opusPlayerActor;

    private OpusLib opusLib;

    private boolean isInited;

    private boolean usedAndroid;
    private long currentId;
    private TelegramApplication application;

    public AudioPlayerActor(TelegramApplication application, ActorSystem system) {
        super(system,"audio_player", "common");
        this.application = application;
        androidPlayerActor = new ClientMessenger(new AndroidPlayerActor(self(), application, system).self(), self());
        opusPlayerActor = new ClientMessenger(new OpusPlayerActor(self(), system).self(), self());
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
            androidPlayerActor.play(currentId, fileName);
            opusPlayerActor.stop();
        } else {
            opusPlayerActor.play(currentId, fileName);
            androidPlayerActor.stop();
        }
        flushNotifications();
    }

    protected void onStopMessage() {
        if (isInited) {
            if (usedAndroid) {
                androidPlayerActor.stop();
            } else {
                opusPlayerActor.stop();
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
                    androidPlayerActor.toggle(id, fileName);
                } else {
                    opusPlayerActor.toggle(id, fileName);
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



    public static class Messenger extends ActorMessenger {

        public Messenger(ActorReference reference, ActorReference sender) {
            super(reference, sender);
        }

        public void play(long id, String fileName) {
            talkRaw("play", id, fileName);
        }

        public void toggle(long id, String fileName) {
            talkRaw("toggle", id, fileName);
        }

        public void stop(long id, String fileName) {
            talkRaw("stop", id, fileName);
        }

        @Override
        public ActorMessenger cloneForSender(ActorReference sender) {
            return new Messenger(reference, sender);
        }
    }

    public static class SubMessenger extends ActorMessenger {
        public SubMessenger(ActorReference reference, ActorReference sender) {
            super(reference, sender);
        }

        public void started(long id) {
            talkRaw("subStart", id);
        }

        public void stoped(long id) {
            talkRaw("subStop", id);
        }

        public void paused(long id, float progress) {
            talkRaw("subPause", id, progress);
        }

        public void progress(long id, float progress) {
            talkRaw("subInProgress", id, progress);
        }

        public void crash(long id) {
            talkRaw("subCrash", id);
        }

        @Override
        public ActorMessenger cloneForSender(ActorReference sender) {
            return new Messenger(reference, sender);
        }
    }

    public static class ClientMessenger extends ActorMessenger {

        public ClientMessenger(ActorReference reference, ActorReference sender) {
            super(reference, sender);
        }

        public void play(long id, String fileName) {
            talkRaw("play", id, fileName);
        }

        public void toggle(long id, String fileName) {
            talkRaw("toggle", id, fileName);
        }

        public void stop() {
            talkRaw("stop");
        }

        @Override
        public ActorMessenger cloneForSender(ActorReference sender) {
            return new ClientMessenger(reference, sender);
        }
    }
}