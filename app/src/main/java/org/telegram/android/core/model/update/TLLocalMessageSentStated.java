package org.telegram.android.core.model.update;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.api.messages.TLAbsStatedMessage;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalMessageSentStated extends TLLocalUpdate {
    private TLAbsStatedMessage absStatedMessage;

    private ChatMessage message;

    public TLLocalMessageSentStated(TLAbsStatedMessage absStatedMessage, ChatMessage message) {
        this.absStatedMessage = absStatedMessage;
        this.message = message;
    }

    public TLAbsStatedMessage getAbsStatedMessage() {
        return absStatedMessage;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
