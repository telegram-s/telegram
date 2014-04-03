package org.telegram.android.kernel;

import android.os.Process;
import org.telegram.android.actors.Actors;
import org.telegram.android.core.audio.AudioPlayerActor;
import org.telegram.android.core.audio.VoiceCaptureActor;
import org.telegram.actors.ActorReference;
import org.telegram.actors.ActorSystem;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class ActorKernel {

    private ActorSystem actorSystem;
    private VoiceCaptureActor.Messenger voiceCaptureActor;
    private AudioPlayerActor.Messenger audioPlayerActor;
    private ApplicationKernel kernel;

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
    }

    public void create() {
        actorSystem = new ActorSystem();

        actorSystem.addThread(Actors.THREAD_FS);
        actorSystem.addThread(Actors.THREAD_ENCODER);
        actorSystem.addThread(Actors.THREAD_AUDIO, Thread.NORM_PRIORITY);
        actorSystem.addThread(Actors.THREAD_COMMON);

        voiceCaptureActor = new VoiceCaptureActor.Messenger(new VoiceCaptureActor(kernel.getApplication(), actorSystem).self(), null);
        audioPlayerActor = new AudioPlayerActor.Messenger(new AudioPlayerActor(kernel.getApplication(), actorSystem).self(), null);
    }

    public VoiceCaptureActor.Messenger getVoiceCaptureActor() {
        return voiceCaptureActor;
    }

    public AudioPlayerActor.Messenger getAudioPlayerActor() {
        return audioPlayerActor;
    }
}
