package org.telegram.threading;

import java.util.ArrayList;

/**
 * Created by ex3ndr on 17.03.14.
 */
public abstract class Actor {
    private ActorReference reference;
    protected ActorSystem actorSystem;
    private ArrayList<MessageKind> kinds = new ArrayList<MessageKind>();

    public Actor(ActorSystem system, String name) {
        ActorThread thread = system.findThread(name);
        if (thread == null) {
            throw new RuntimeException("Unable to find thread '" + name + "'");
        }
        reference = new ActorReference(this, thread);
        actorSystem = system;

        registerMethods();
    }

    protected void registerMethods() {

    }

    protected void registerKind(String name, Class... args) {
        kinds.add(new MessageKind(name, args));
    }

    public final void receiveMessage(String name, Object[] args, ActorReference sender) throws Exception {
        outer:
        for (MessageKind kind : kinds) {
            if (kind.getArgs().length != args.length) {
                continue;
            }

            if (!kind.getName().equals(name)) {
                continue;
            }

            for (int i = 0; i < kind.getArgs().length; i++) {
                if (args[i] == null) {
                    continue;
                }
                if (kind.getArgs()[i].isPrimitive()) {
                    continue;
                }
                if (!kind.getArgs()[i].isAssignableFrom(args[i].getClass())) {
                    continue outer;
                }
            }

            receive(name, args, sender);
        }
    }

    protected abstract void receive(String name, Object[] args, ActorReference sender) throws Exception;

    public ActorReference self() {
        return reference;
    }

    public void onException(Exception e) {

    }

    protected void destroy() {

    }

    private class MessageKind {
        private String name;
        private Class[] args;

        private MessageKind(String name, Class[] args) {
            this.name = name;
            this.args = args;
        }

        public String getName() {
            return name;
        }

        public Class[] getArgs() {
            return args;
        }
    }
}
