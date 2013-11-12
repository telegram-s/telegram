package org.telegram.android.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 18:57
 */
public abstract class AsyncAction {

    public void beforeExecute() {
    }

    public void afterExecute() {

    }

    public abstract void execute() throws AsyncException;

    public void onException(AsyncException e) {

    }

    public void onCanceled() {

    }

    public boolean repeatable() {
        return false;
    }
}