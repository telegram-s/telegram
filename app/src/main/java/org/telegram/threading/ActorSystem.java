package org.telegram.threading;

import android.os.Process;
import org.telegram.android.log.Logger;

import java.util.HashMap;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class ActorSystem {
    private static final String TAG = "ActorSystem";
    private HashMap<String, ActorThread> threads;

    public ActorSystem() {
        threads = new HashMap<String, ActorThread>();
    }

    public void addThread(String name, int priority) {
        threads.put(name, new ActorThread(name, priority));
    }

    public void addThread(String name) {
        threads.put(name, new ActorThread(name, Process.THREAD_PRIORITY_BACKGROUND));
    }

    public ActorThread findThread(String name) {
        return threads.get(name);
    }

    public void runThreads() {
        for (ActorThread thread : threads.values()) {
            thread.start();
        }
    }

    public void onUnhandledMessage(Actor actor, String name, Object[] args, ActorReference reference) {
        Logger.w(TAG, "Unhandled message " + name + " for actor " + actor);
    }
}