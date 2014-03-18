package org.telegram.android.core.audio;

import org.telegram.android.TelegramApplication;
import org.telegram.opus.OpusLib;
import org.telegram.threading.Actor;
import org.telegram.threading.ActorSystem;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class AudioPlayerActor extends Actor<AudioPlayerActor.Message> {
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private static final int STATE_PAUSED = 2;

    private int state = STATE_NONE;

    private boolean useAndroidPlayer;

    private AndroidPlayerActor androidPlayerActor;
    private OpusPlayerActor opusPlayerActor;

    private OpusLib opusLib;

    public AudioPlayerActor(TelegramApplication application, ActorSystem system) {
        super(system, "common");
        androidPlayerActor = new AndroidPlayerActor(application, system);
        opusPlayerActor = new OpusPlayerActor(system);
        opusLib = new OpusLib();
    }

    @Override
    public void receive(Message message, Actor sender) throws Exception {
        if (message instanceof PlayAudio) {
            String fileName = ((PlayAudio) message).getFileName();
            if (opusLib.isOpusFile(fileName) > 0) {
                opusPlayerActor.sendMessage(new OpusPlayerActor.PlayAudio(fileName));
            } else {
                androidPlayerActor.sendMessage(new AndroidPlayerActor.PlayAudio(fileName));
            }
        } else if (message instanceof StopAudio) {
            androidPlayerActor.sendMessage(new AndroidPlayerActor.StopAudio());
        } else if (message instanceof ToggleAudio) {

        }
    }

    public abstract static class Message {

    }

    public static class PlayAudio extends Message {
        private String fileName;

        public PlayAudio(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class StopAudio extends Message {

    }

    public static class ToggleAudio extends Message {

    }
}
