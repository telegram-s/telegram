package org.telegram.android.core.model.update;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.api.messages.TLAbsSentMessage;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalMessageSent extends TLLocalUpdate {
    private TLAbsSentMessage absSentMessage;
    private ChatMessage message;

    public TLLocalMessageSent(TLAbsSentMessage absSentMessage, ChatMessage message) {
        this.absSentMessage = absSentMessage;
        this.message = message;
    }

    public TLAbsSentMessage getAbsSentMessage() {
        return absSentMessage;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
