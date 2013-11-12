package org.telegram.android.tasks;

/**
 * Author: Korshakov Stepan
 * Created: 25.07.13 3:20
 */
public interface RecoverCallback {
    public void onError(AsyncException e, Runnable onRepeat, Runnable onCancel);
}
