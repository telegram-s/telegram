package org.telegram.notifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 18.03.14.
 */
public class Notifications {
    private HashMap<Integer, State> states = new HashMap<Integer, State>();
    private ConcurrentHashMap<Integer, ArrayList<StateSubscriber>> stateSubscribers = new ConcurrentHashMap<Integer, ArrayList<StateSubscriber>>();

    public void registerSubscriber(StateSubscriber stateSubscriber, int id) {
        // stateSubscribers.put()
    }

    private class State {
        private int kind;
        private long id;
        private Object[] args;

        private State(int kind, long id, Object[] args) {
            this.kind = kind;
            this.id = id;
            this.args = args;
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
}