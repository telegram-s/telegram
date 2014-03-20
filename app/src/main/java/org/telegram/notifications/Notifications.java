package org.telegram.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class Notifications {
    public static final int KIND_ANY = 0;
    public static final int KIND_START = 1;

    private HashMap<Integer, HashMap<Long, State>> states = new HashMap<Integer, HashMap<Long, State>>();

    private ConcurrentHashMap<Integer, HashSet<EventSubscriber>> eventSubscribers = new ConcurrentHashMap<Integer, HashSet<EventSubscriber>>();
    private ConcurrentHashMap<EventSubscriber, EventSubscriberDef> subscribersMap = new ConcurrentHashMap<EventSubscriber, EventSubscriberDef>();

    private HashSet<StateSubscriberDef> stateSubscribers = new HashSet<StateSubscriberDef>();

    private final NotificationDispatcher dispatcher;
    private ArrayList<PendingState> pendingStates = new ArrayList<PendingState>();

    public Notifications(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void sendEvent(final int kind, final Object... args) {
        dispatcher.dispatchNotification(new Runnable() {
            @Override
            public void run() {
                HashSet<EventSubscriber> subscribers = eventSubscribers.get(kind);
                if (subscribers != null) {
                    for (EventSubscriber subscriber : subscribers) {
                        subscriber.onEvent(kind, args);
                    }
                }
                if (kind != KIND_ANY) {
                    subscribers = eventSubscribers.get(KIND_ANY);
                    if (subscribers != null) {
                        for (EventSubscriber subscriber : subscribers) {
                            subscriber.onEvent(kind, args);
                        }
                    }
                }
            }
        });
    }

    public void sendState(final int kind, final long id, final int state, final Object... args) {
        dispatcher.dispatchNotification(new Runnable() {
            @Override
            public void run() {
                if (pendingStates.size() > 0) {
                    for (PendingState state : pendingStates) {
                        sendPendingState(state.kind, state.id, state.state, state.isUpdate, state.args);
                    }
                    pendingStates.clear();
                }
                sendPendingState(kind, id, state, false, args);
            }
        });
    }

    public void sendStateUpdate(final int kind, final long id, final int state, final Object... args) {
        dispatcher.dispatchNotification(new Runnable() {
            @Override
            public void run() {
                if (pendingStates.size() > 0) {
                    for (PendingState state : pendingStates) {
                        sendPendingState(state.kind, state.id, state.state, state.isUpdate, state.args);
                    }
                    pendingStates.clear();
                }
                sendPendingState(kind, id, state, true, args);
            }
        });
    }

    public synchronized void sendDelayedState(final int kind, final long id, final int state, final Object... args) {
        pendingStates.add(new PendingState(kind, id, state, false, args));
    }

    public void flushPending() {
        dispatcher.dispatchNotification(new Runnable() {
            @Override
            public void run() {
                if (pendingStates.size() > 0) {
                    for (PendingState state : pendingStates) {
                        sendPendingState(state.kind, state.id, state.state, state.isUpdate, state.args);
                    }
                    pendingStates.clear();
                }
            }
        });
    }

    private synchronized void sendPendingState(final int kind, final long id, final int state, boolean update, final Object... args) {
        HashMap<Long, State> stateHashMap = states.get(kind);
        if (stateHashMap == null) {
            stateHashMap = new HashMap<Long, State>();
            states.put(kind, stateHashMap);
        }
        if (update) {
            State oldState = stateHashMap.get(id);
            if (oldState == null || oldState.state != state) {
                return;
            }
        }
        stateHashMap.put(id, new State(kind, id, state, args));
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.anyKinds.contains(KIND_ANY)) {
                def.subscriber.onStateChanged(kind, id, state, args);
            } else if (def.getAnyKinds().contains(kind)) {
                def.subscriber.onStateChanged(kind, id, state, args);
            } else {
                ArrayList<Long> res = def.kinds.get(kind);
                if (res != null && res.contains(id)) {
                    def.subscriber.onStateChanged(kind, id, state, args);
                }
            }
        }
    }

    public synchronized void registerEventSubscriber(EventSubscriber eventSubscriber, int kind) {
        if (kind < 0) {
            throw new RuntimeException("Unsupported event type: " + kind);
        }
        EventSubscriberDef def = subscribersMap.get(eventSubscriber);
        if (def == null) {
            def = new EventSubscriberDef(eventSubscriber);
            def.getKinds().add(kind);
            subscribersMap.putIfAbsent(eventSubscriber, def);
        } else {
            def.getKinds().add(kind);
        }

        HashSet<EventSubscriber> subscribers = eventSubscribers.get(kind);
        if (subscribers == null) {
            subscribers = new HashSet<EventSubscriber>();
            subscribers.add(eventSubscriber);
            eventSubscribers.put(kind, subscribers);
        } else {
            subscribers.add(eventSubscriber);
        }
    }

    public synchronized void unregisterEventSubscriber(EventSubscriber subscriber) {
        EventSubscriberDef def = subscribersMap.get(subscriber);
        if (def != null) {
            for (Integer i : def.getKinds()) {
                eventSubscribers.get(i).remove(subscriber);
            }
            def.getKinds().clear();
        }
    }

    public synchronized void unregisterSubscriber(StateSubscriber stateSubscriber, int kind, long id) {
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.subscriber == stateSubscriber) {
                if (def.kinds.containsKey(kind)) {
                    ArrayList<Long> ids = def.kinds.get(kind);
                    ids.remove(id);
                }
                return;
            }
        }
    }

    public synchronized void unregisterSubscriber(StateSubscriber stateSubscriber, int kind) {
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.subscriber == stateSubscriber) {
                def.anyKinds.remove(kind);
                def.kinds.remove(kind);
                return;
            }
        }
    }

    public synchronized void unregisterSubscriber(StateSubscriber stateSubscriber) {
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.subscriber == stateSubscriber) {
                stateSubscribers.remove(def);
                return;
            }
        }
    }

    public synchronized void registerSubscriber(StateSubscriber stateSubscriber, int kind, long id) {
        boolean isBinded = false;
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.subscriber == stateSubscriber) {
                ArrayList<Long> ids = def.kinds.get(kind);
                if (ids != null) {
                    ids.add(id);
                } else {
                    ids = new ArrayList<Long>();
                    ids.add(id);
                    def.kinds.put(kind, ids);
                }
                isBinded = true;
                break;
            }
        }

        if (!isBinded) {
            StateSubscriberDef def = new StateSubscriberDef(stateSubscriber);
            ArrayList<Long> ids = new ArrayList<Long>();
            ids.add(id);
            def.kinds.put(kind, ids);
            stateSubscribers.add(def);
        }

        State lastState = null;
        HashMap<Long, State> stateHashMap = states.get(kind);
        if (stateHashMap != null) {
            lastState = stateHashMap.get(id);
        }
        if (lastState != null) {
            stateSubscriber.onStateChanged(kind, id, lastState.getState(), lastState.args);
        }
    }

    public synchronized void registerSubscriber(StateSubscriber stateSubscriber, int kind) {
        for (StateSubscriberDef def : stateSubscribers) {
            if (def.subscriber == stateSubscriber) {
                def.anyKinds.add(kind);
                return;
            }
        }

        StateSubscriberDef def = new StateSubscriberDef(stateSubscriber);
        def.anyKinds.add(kind);
        stateSubscribers.add(def);
    }

    public synchronized void registerSubscriber(StateSubscriber stateSubscriber) {
        registerSubscriber(stateSubscriber, KIND_ANY);
    }

    private class EventSubscriberDef {
        private EventSubscriber subscriber;
        private HashSet<Integer> kinds;

        private EventSubscriberDef(EventSubscriber subscriber) {
            this.subscriber = subscriber;
            this.kinds = new HashSet<Integer>();
        }

        public EventSubscriber getSubscriber() {
            return subscriber;
        }

        public HashSet<Integer> getKinds() {
            return kinds;
        }
    }

    private class StateSubscriberDef {
        private StateSubscriber subscriber;
        private HashSet<Integer> anyKinds;
        private HashMap<Integer, ArrayList<Long>> kinds;

        private StateSubscriberDef(StateSubscriber subscriber) {
            this.subscriber = subscriber;
            this.anyKinds = new HashSet<Integer>();
            this.kinds = new HashMap<Integer, ArrayList<Long>>();
        }

        public StateSubscriber getSubscriber() {
            return subscriber;
        }

        public HashSet<Integer> getAnyKinds() {
            return anyKinds;
        }

        public HashMap<Integer, ArrayList<Long>> getKinds() {
            return kinds;
        }

        public ArrayList<Long> getKindIds(int kind) {
            if (kinds.containsKey(kind)) {
                return kinds.get(kind);
            } else {
                return kinds.put(kind, new ArrayList<Long>());
            }
        }
    }

    private class State {
        private int kind;
        private long id;
        private int state;
        private Object[] args;

        private State(int kind, long id, int state, Object[] args) {
            this.kind = kind;
            this.id = id;
            this.args = args;
            this.state = state;
        }

        public int getState() {
            return state;
        }

        public int getKind() {
            return kind;
        }

        public long getId() {
            return id;
        }

        public Object[] getArgs() {
            return args;
        }
    }

    private class PendingState {
        private int kind;
        private long id;
        private int state;
        private Object[] args;
        private boolean isUpdate;

        private PendingState(int kind, long id, int state, boolean isUpdate, Object[] args) {
            this.kind = kind;
            this.id = id;
            this.state = state;
            this.args = args;
            this.isUpdate = isUpdate;
        }

        public boolean isUpdate() {
            return isUpdate;
        }

        public int getKind() {
            return kind;
        }

        public long getId() {
            return id;
        }

        public int getState() {
            return state;
        }

        public Object[] getArgs() {
            return args;
        }
    }
}