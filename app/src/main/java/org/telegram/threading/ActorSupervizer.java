package org.telegram.threading;

/**
 * Created by ex3ndr on 17.03.14.
 */
public interface ActorSupervizer {
    void onActorDie(Actor actor, Exception e);
}
