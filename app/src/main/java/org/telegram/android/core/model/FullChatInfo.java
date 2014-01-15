package org.telegram.android.core.model;

import org.telegram.android.core.model.local.TLLocalFullChatInfo;

/**
 * Author: Korshakov Stepan
 * Created: 06.08.13 11:48
 */
public class FullChatInfo {
    private int chatId;
    private TLLocalFullChatInfo chatInfo;

    public FullChatInfo(int chatId, TLLocalFullChatInfo chatInfo) {
        this.chatId = chatId;
        this.chatInfo = chatInfo;
    }

    public int getChatId() {
        return chatId;
    }

    public TLLocalFullChatInfo getChatInfo() {
        return chatInfo;
    }
}
