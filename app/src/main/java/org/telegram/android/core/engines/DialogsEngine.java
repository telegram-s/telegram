package org.telegram.android.core.engines;

import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.DialogDescription;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class DialogsEngine {
    private DialogsDatabase database;
    private StelsApplication application;

    public DialogsEngine(ModelEngine engine) {
        this.application = engine.getApplication();
        this.database = new DialogsDatabase(engine);
    }

    public DialogDescription[] getItems(int offset, int limit) {
        return database.getItems(offset, limit);
    }

    public DialogDescription[] getAll() {
        return database.getAll();
    }

    public DialogDescription[] getUnreadedRemotelyDescriptions() {
        return database.getUnreadedRemotelyDescriptions();
    }

    public DialogDescription loadDialog(int peerType, int peerId) {
        return database.loadDialog(peerType, peerId);
    }

    public void updateOrCreateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public void updateDialog(DialogDescription description) {
        database.updateOrCreateDialog(description);
        application.getDialogSource().getViewSource().updateItem(description);
    }

    public void deleteDialog(DialogDescription description) {
        database.deleteDialog(description);
        application.getDialogSource().getViewSource().removeItem(description);
    }
}