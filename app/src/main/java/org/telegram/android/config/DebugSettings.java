package org.telegram.android.config;

import android.content.Context;
import com.extradea.framework.persistence.ContextPersistence;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 14:41
 */
public class DebugSettings extends ContextPersistence {

    public static final int LAYER_DEFAULT = 0;
    public static final int LAYER_NONE = 1;
    public static final int LAYER_HARDWARE = 2;
    public static final int LAYER_SOFTWARE = 3;

    private int dialogListLayerType = 0;
    private int dialogListItemLayerType = 0;

    private int conversationListLayerType = 0;
    private int conversationListItemLayerType = 0;

    private boolean forceAnimations = false;

    private boolean isDeveloperMode = false;

    private boolean isSaveLogs = false;

    public DebugSettings(Context context) {
        super(context);
        tryLoad();
    }

    public int getDialogListLayerType() {
        return dialogListLayerType;
    }

    public void setDialogListLayerType(int dialogListLayerType) {
        this.dialogListLayerType = dialogListLayerType;
        trySave();
    }

    public int getDialogListItemLayerType() {
        return dialogListItemLayerType;
    }

    public void setDialogListItemLayerType(int dialogListItemLayerType) {
        this.dialogListItemLayerType = dialogListItemLayerType;
        trySave();
    }

    public int getConversationListLayerType() {
        return conversationListLayerType;
    }

    public void setConversationListLayerType(int conversationListLayerType) {
        this.conversationListLayerType = conversationListLayerType;
        trySave();
    }

    public int getConversationListItemLayerType() {
        return conversationListItemLayerType;
    }

    public void setConversationListItemLayerType(int conversationListItemLayerType) {
        this.conversationListItemLayerType = conversationListItemLayerType;
        trySave();
    }

    public boolean isForceAnimations() {
        return forceAnimations;
    }

    public void setForceAnimations(boolean forceAnimations) {
        this.forceAnimations = forceAnimations;
        trySave();
    }

    public boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public void setDeveloperMode(boolean developerMode) {
        isDeveloperMode = developerMode;
        trySave();
    }

    public boolean isSaveLogs() {
        return isSaveLogs;
    }

    public void setSaveLogs(boolean saveLogs) {
        isSaveLogs = saveLogs;
        trySave();
    }
}
