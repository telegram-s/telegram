package org.telegram.threading;

/**
 * Created by ex3ndr on 17.03.14.
 */
public class ActorReference {
    private ActorThread thread;
    private Actor actor;

    public ActorReference(Actor actor, ActorThread thread) {
        this.actor = actor;
        this.thread = thread;
    }

    public void deliverMessage(Object message, Actor sender) {
        thread.deliverMessage(actor, message, sender);
    }

    public void deliverMessageDelayed(Object message, Actor sender, long delay) {
        thread.deliverMessageDelayed(actor, message, sender, delay);
    }
}
