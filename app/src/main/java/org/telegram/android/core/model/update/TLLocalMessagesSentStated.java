package org.telegram.android.core.model.update;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.api.messages.TLAbsStatedMessages;

/**
 * Created by ex3ndr on 26.11.13.
 */
public class TLLocalMessagesSentStated extends TLLocalUpdate {
    private TLAbsStatedMessages absStatedMessages;

    private ChatMessage message;

    public TLLocalMessagesSentStated(TLAbsStatedMessages absStatedMessages, ChatMessage message) {
        this.absStatedMessages = absStatedMessages;
        this.message = message;
    }

    public TLAbsStatedMessages getAbsStatedMessages() {
        return absStatedMessages;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
