package org.telegram.android.core.model;

import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;

/**
 * Created by ex3ndr on 03.01.14.
 */
public class Group {
    private int chatId;
    private String title;
    private TLAbsLocalAvatarPhoto avatar;

    public Group(int chatId, String title, TLAbsLocalAvatarPhoto avatar) {
        this.chatId = chatId;
        this.title = title;
        this.avatar = avatar;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TLAbsLocalAvatarPhoto getAvatar() {
        return avatar;
    }

    public void setAvatar(TLAbsLocalAvatarPhoto avatar) {
        this.avatar = avatar;
    }
}