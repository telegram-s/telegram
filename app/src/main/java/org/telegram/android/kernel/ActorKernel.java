package org.telegram.android.kernel;

import android.os.Process;
import org.telegram.android.core.audio.VoiceCaptureActor;
import org.telegram.threading.ActorSystem;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class ActorKernel {

    private ActorSystem actorSystem;
    private VoiceCaptureActor voiceCaptureActor;
    private ApplicationKernel kernel;

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorKernel(ApplicationKernel kernel) {
        this.kernel = kernel;
    }

    public void create() {
        actorSystem = new ActorSystem();

        actorSystem.addThread("fs");
        actorSystem.addThread("encoding");
        actorSystem.addThread("audio", Process.THREAD_PRIORITY_AUDIO);

        voiceCaptureActor = new VoiceCaptureActor(kernel.getApplication(), actorSystem);
    }

    public VoiceCaptureActor getVoiceCaptureActor() {
        return voiceCaptureActor;
    }

    public void runKernel() {
        actorSystem.runThreads();
    }
}
