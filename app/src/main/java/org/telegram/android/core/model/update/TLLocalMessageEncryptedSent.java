package org.telegram.android.core.model.update;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.api.messages.TLAbsSentEncryptedMessage;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalMessageEncryptedSent extends TLLocalUpdate {
    private TLAbsSentEncryptedMessage encryptedMessage;
    private ChatMessage message;

    public TLLocalMessageEncryptedSent(TLAbsSentEncryptedMessage encryptedMessage, ChatMessage message) {
        this.encryptedMessage = encryptedMessage;
        this.message = message;
    }

    public TLAbsSentEncryptedMessage getEncryptedMessage() {
        return encryptedMessage;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
