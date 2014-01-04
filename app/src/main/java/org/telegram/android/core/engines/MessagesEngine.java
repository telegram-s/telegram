package org.telegram.android.core.engines;

import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.ChatMessage;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class MessagesEngine {
    private ModelEngine engine;
    private MessagesDatabase database;
    private StelsApplication application;

    public MessagesEngine(ModelEngine engine) {
        this.engine = engine;
        this.database = new MessagesDatabase(engine);
        this.application = engine.getApplication();
    }

    public ChatMessage getMessageByMid(int mid) {
        return database.getMessageByMid(mid);
    }

    public ChatMessage[] getMessagesByMid(int[] mids) {
        return database.getMessagesByMid(mids);
    }

    public ChatMessage getMessageById(int id) {
        return database.getMessageById(id);
    }

    public ChatMessage getMessageByRandomId(long rid) {
        return database.getMessageByRid(rid);
    }

    public void create(ChatMessage message) {
        database.create(message);
        application.getDataSourceKernel().onSourceAddMessage(message);
    }

    public void update(ChatMessage message) {
        database.update(message);
        application.getDataSourceKernel().onSourceUpdateMessage(message);
    }
}
