package org.telegram.android.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 24.07.13 1:24
 */
public interface TaskCallback<V> {
    public void onException(Exception e);

    public void onSuccess(V value);

    public void onConfirmed();

    public void onCanceled();

    public void onTimedOut();
}
