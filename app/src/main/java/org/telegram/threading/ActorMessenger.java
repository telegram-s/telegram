package org.telegram.threading;

/**
 * Created by ex3ndr on 24.03.14.
 */
public abstract class ActorMessenger {
    protected ActorReference reference;
    protected ActorReference sender;

    protected ActorMessenger(ActorReference reference, ActorReference sender) {
        this.reference = reference;
        this.sender = sender;
    }

    public void talkRaw(String message, Object... args) {
        reference.talk(message, sender, args);
    }

    public void talkRawDelayed(String message, long delay, Object... args) {
        reference.talkDelayed(message, sender, delay, args);
    }

    public abstract ActorMessenger cloneForSender(ActorReference sender);
}
