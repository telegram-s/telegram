package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.android.TelegramApplication;
import org.telegram.api.TLConfig;

/**
 * Created by ex3ndr on 09.12.13.
 */
public class SystemConfig {

    private static final String INVITE_MESSAGE_DEFAULT = "Hey, let's switch to Telegram\n" +
            "http://telegram.org/dl1";

    private SharedPreferences preferences;

    private TelegramApplication application;
    private int maxChatSize = 100;

    private String inviteMessage;
    private String inviteMessageLang;

    public SystemConfig(TelegramApplication application) {
        this.application = application;
        preferences = application.getSharedPreferences("org.telegram.android.SystemConfig.pref", Context.MODE_PRIVATE);
        maxChatSize = preferences.getInt("max_chat_size", 100);
        inviteMessage = preferences.getString("invite_message", INVITE_MESSAGE_DEFAULT);
        inviteMessageLang = preferences.getString("invite_message_lang", "");
    }

    public int getMaxChatSize() {
        return maxChatSize;
    }

    public String getInviteMessage() {
        return inviteMessage;
    }

    public String getInviteMessageLang() {
        return inviteMessageLang;
    }

    public void onConfig(TLConfig config) {
        maxChatSize = config.getChatSizeMax();
        preferences.edit().putInt("max_chat_size", maxChatSize).commit();
    }

    public void onInviteMessage(String message, String lang) {
        this.inviteMessage = message;
        this.inviteMessageLang = lang;
        preferences.edit()
                .putString("invite_message", inviteMessage)
                .putString("invite_message_lang", inviteMessageLang).commit();
    }
}
