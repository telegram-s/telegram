package org.telegram.threading;

/**
 * Created by ex3ndr on 17.03.14.
 */
public abstract class Actor<T> {
    private ActorReference reference;
    protected ActorSystem actorSystem;
    private boolean isAlive = true;

    public Actor(ActorSystem system, String name) {
        ActorThread thread = system.findThread(name);
        if (thread == null) {
            throw new RuntimeException("Unable to find thread '" + name + "'");
        }
        reference = new ActorReference(this, thread);
        actorSystem = system;
    }

    public abstract void receive(T message, Actor sender) throws Exception;

    public void onException(Exception e) {

    }

    public synchronized final void kill() {
        if (!isAlive) {
            return;
        }
        isAlive = false;
        destroy();
    }

    public boolean isAlive() {
        return isAlive;
    }

    protected void destroy() {

    }

    public final void sendMessage(T message, Actor sender) {
        reference.deliverMessage(message, sender);
    }

    public final void sendMessage(T message) {
        sendMessage(message, null);
    }

    public final void sendMessage(T message, Actor sender, long delay) {
        reference.deliverMessageDelayed(message, sender, delay);
    }

    public final void sendMessage(T message, long delay) {
        sendMessage(message, null, delay);
    }
}
