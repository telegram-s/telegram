package org.telegram.android.core;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.09.13
 * Time: 17:09
 */
public class DynamicConfig extends ContextPersistence {

    private static final String INVITE_MESSAGE_DEFAULT = "Hey, let's switch to Telegram\n" +
            "http://telegram.org/dl1";
    private String inviteMessage = INVITE_MESSAGE_DEFAULT;
    private String inviteMessageLang = "";

    public DynamicConfig(Context context) {
        super(context, true);
        tryLoad();
    }

    public String getInviteMessage() {
        return inviteMessage;
    }

    public String getInviteMessageLang() {
        return inviteMessageLang;
    }

    public void setInviteMessage(String lang, String inviteMessage) {
        this.inviteMessage = inviteMessage;
        this.inviteMessageLang = lang;
        trySave();
    }
}
