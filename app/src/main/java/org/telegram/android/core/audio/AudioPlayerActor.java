package org.telegram.android.core.audio;

import android.widget.Toast;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.Events;
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
        opusPlayerActor = new OpusPlayerActor(this, system);
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
        } else if (message instanceof SubPlayerStart) {
            if (((SubPlayerStart) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_IN_PROGRESS, 0.0f);
        } else if (message instanceof SubPlayerStop) {
            if (((SubPlayerStop) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_STOP);
        } else if (message instanceof SubPlayerPaused) {
            if (((SubPlayerPaused) message).id != currentId || !isInited) {
                return;
            }
            notify(Events.STATE_PAUSED, ((SubPlayerPaused) message).progress);
        } else if (message instanceof SubPlayerInProgress) {
            if (((SubPlayerInProgress) message).id != currentId || !isInited) {
                return;
            }
            notifyUpdate(Events.STATE_IN_PROGRESS, ((SubPlayerInProgress) message).progress);
        } else if (message instanceof SubPlayerCrash) {
            if (((SubPlayerCrash) message).id != currentId || !isInited) {
                return;
            }

            Toast.makeText(application, "File error", Toast.LENGTH_SHORT).show();
            notify(Events.STATE_STOP);
        }

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

    private void play(long id, String fileName) {
        this.fileName = fileName;
        this.usedAndroid = opusLib.isOpusFile(fileName) <= 0;
        this.currentId = id;
        this.isInited = true;

        if (usedAndroid) {
            androidPlayerActor.sendMessage(new AndroidPlayerActor.PlayAudio(currentId, fileName));
            opusPlayerActor.sendMessage(new OpusPlayerActor.StopAudio());
        } else {
            opusPlayerActor.sendMessage(new OpusPlayerActor.PlayAudio(currentId, fileName));
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

    public static class SubPlayerStart extends Message {
        private long id;

        public SubPlayerStart(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class SubPlayerInProgress extends Message {
        private long id;
        private float progress;

        public SubPlayerInProgress(long id, float progress) {
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

    public static class SubPlayerStop extends Message {
        private long id;

        public SubPlayerStop(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class SubPlayerPaused extends Message {
        private long id;
        private float progress;

        public SubPlayerPaused(long id, float progress) {
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


    public static class SubPlayerCrash extends Message {
        private long id;

        public SubPlayerCrash(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }
}