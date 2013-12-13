package org.telegram.android.core.wireframes;

import org.telegram.android.core.model.ChatMessage;
import org.telegram.android.core.model.User;

/**
 * Created by ex3ndr on 12.12.13.
 */
public class MessageWireframe {
    public ChatMessage message;

    public long randomId;
    public int databaseId;
    public int mid;
    public int peerId;
    public int peerType;
    public int dateValue;
    public String date;

    public User senderUser;
    public User forwardUser;
}
