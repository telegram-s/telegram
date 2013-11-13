package org.telegram.android.core;

import org.telegram.android.core.model.DialogDescription;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 0:16
 */
public interface ChatSourceListener {
    public void onChatChanged(int chatId, DialogDescription description);
}
