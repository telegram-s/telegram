package org.telegram.android.core.model.update;

import org.telegram.api.messages.TLAbsStatedMessage;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalRemoveChatUser extends TLLocalUpdate {
    private TLAbsStatedMessage message;

    public TLLocalRemoveChatUser(TLAbsStatedMessage message) {
        this.message = message;
    }

    public TLAbsStatedMessage getMessage() {
        return message;
    }
}
