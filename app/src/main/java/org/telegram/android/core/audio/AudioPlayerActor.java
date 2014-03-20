package org.telegram.android.core.audio;

import android.widget.Toast;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
import org.telegram.notifications.Notifications;
import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AudioPlayerActor extends Actor<AudioPlayerActor.Message> {
    private AndroidPlayerActor androidPlayerActor;
    private OpusPlayerActor opusPlayerActor;

    private OpusLib opusLib;

    private boolean isInited;
    private String fileName;
    private boolean usedAndroid;
    private long currentId;
    private TelegramApplication application;

    public AudioPlayerActor(TelegramApplication application, ActorSystem system) {
        super(system, "common");
        this.application = application;
        androidPlayerActor = new AndroidPlayerActor(this, application, system);
        opusPlayerActor = new OpusPlayerActor(system);
        opusLib = new OpusLib();
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            if (isInited) {
                stop();
            }
            play(((PlayAudio) message).id, ((PlayAudio) message).fileName);
            flushNotifications();
        } else if (message instanceof StopAudio) {
            stop();
        } else if (message instanceof ToggleAudio) {
            if (isInited) {
                if (((ToggleAudio) message).id == currentId) {
                    if (usedAndroid) {
                        androidPlayerActor.sendMessage(new AndroidPlayerActor.ToggleAudio());
                    } else {
                        opusPlayerActor.sendMessage(new OpusPlayerActor.ToggleAudio());
                    }
                } else {
                    stop();
                }
            }

            if (!isInited) {
                play(((ToggleAudio) message).getId(), ((ToggleAudio) message).getFileName());
            }
            flushNotifications();
        } else if (message instanceof AndroidPlayerStart) {
            if (((AndroidPlayerStart) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_IN_PROGRESS, 0.0f);
        } else if (message instanceof AndroidPlayerStop) {
            if (((AndroidPlayerStop) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_STOP);
        } else if (message instanceof AndroidPlayerPaused) {
            if (((AndroidPlayerPaused) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_PAUSED, ((AndroidPlayerPaused) message).progress);
        } else if (message instanceof AndroidPlayerInProgress) {
            if (((AndroidPlayerInProgress) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_IN_PROGRESS, ((AndroidPlayerInProgress) message).progress);
        } else if (message instanceof AndroidPlayerCrash) {
            if (((AndroidPlayerCrash) message).id != currentId || !isInited) {
                return;
            }

            Toast.makeText(application, "File error", Toast.LENGTH_SHORT).show();
            notify(Events.STATE_STOP);
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

    private void play(long id, String fileName) {
        this.fileName = fileName;
        this.usedAndroid = opusLib.isOpusFile(fileName) <= 0;
        this.usedAndroid = true;
        this.currentId = id;
        this.isInited = true;

        if (usedAndroid) {
            androidPlayerActor.sendMessage(new AndroidPlayerActor.PlayAudio(currentId, fileName));
            opusPlayerActor.sendMessage(new OpusPlayerActor.StopAudio());
        } else {
            opusPlayerActor.sendMessage(new OpusPlayerActor.PlayAudio(fileName));
            androidPlayerActor.sendMessage(new AndroidPlayerActor.StopAudio());
        }
    }

    private void stop() {
        if (isInited) {
            if (usedAndroid) {
                androidPlayerActor.sendMessage(new AndroidPlayerActor.StopAudio());
            } else {
                opusPlayerActor.sendMessage(new OpusPlayerActor.StopAudio());
            }
            notifyPending(Events.STATE_STOP);
        }

        currentId = -1;
        isInited = false;
    }

    public abstract static class Message {

    }

    public static class PlayAudio extends Message {
        private String fileName;
        private long id;

        public PlayAudio(long id, String fileName) {
            this.id = id;
            this.fileName = fileName;
        }

        public long getId() {
            return id;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class StopAudio extends Message {

    }

    public static class ToggleAudio extends Message {
        private String fileName;
        private long id;

        public ToggleAudio(long id, String fileName) {
            this.fileName = fileName;
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public String getFileName() {
            return fileName;
        }
    }

    // Internal

    public static class AndroidPlayerStart extends Message {
        private long id;

        public AndroidPlayerStart(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class AndroidPlayerInProgress extends Message {
        private long id;
        private float progress;

        public AndroidPlayerInProgress(long id, float progress) {
            this.id = id;
            this.progress = progress;
        }

        public float getProgress() {
            return progress;
        }

        public long getId() {
            return id;
        }
    }

    public static class AndroidPlayerStop extends Message {
        private long id;

        public AndroidPlayerStop(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class AndroidPlayerPaused extends Message {
        private long id;
        private float progress;

        public AndroidPlayerPaused(long id, float progress) {
            this.id = id;
            this.progress = progress;
        }

        public float getProgress() {
            return progress;
        }

        public long getId() {
            return id;
        }
    }

    public static class OpusPlayerStart extends Message {
        private long id;

        public OpusPlayerStart(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class OpusPlayerStop extends Message {
        private long id;

        public OpusPlayerStop(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class AndroidPlayerCrash extends Message {
        private long id;

        public AndroidPlayerCrash(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class OpusPlayerCrash extends Message {
        private long id;

        public OpusPlayerCrash(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }
}