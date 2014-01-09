package org.telegram.android.core;

import org.telegram.android.core.model.User;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 3:37
 */
public interface UserSourceListener {
    public void onUsersChanged(User[] users);
}
