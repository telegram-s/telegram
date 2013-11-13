package org.telegram.android.core;

import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.EncryptedChat;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 0:16
 */
public interface EncryptedChatSourceListener {
    public void onEncryptedChatChanged(int chatId, EncryptedChat chat);
}
