package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Author: Korshakov Stepan
 * Created: 12.09.13 14:41
 */
public class DebugSettings {

    private SharedPreferences preferences;

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
        preferences = context.getSharedPreferences("org.telegram.android.DebugSettings.pref", Context.MODE_PRIVATE);
        isSaveLogs = preferences.getBoolean("isSaveLogs", false);
        isDeveloperMode = preferences.getBoolean("isDeveloperMode", false);
        forceAnimations = preferences.getBoolean("forceAnimations", false);
        conversationListLayerType = preferences.getInt("conversationListLayerType", 0);
        conversationListItemLayerType = preferences.getInt("conversationListItemLayerType", 0);
        dialogListLayerType = preferences.getInt("dialogListLayerType", 0);
        dialogListItemLayerType = preferences.getInt("dialogListItemLayerType", 0);
    }

    public int getDialogListLayerType() {
        return dialogListLayerType;
    }

    public synchronized void setDialogListLayerType(int dialogListLayerType) {
        this.dialogListLayerType = dialogListLayerType;
        preferences.edit().putInt("dialogListLayerType", dialogListLayerType).commit();
    }

    public int getDialogListItemLayerType() {
        return dialogListItemLayerType;
    }

    public synchronized void setDialogListItemLayerType(int dialogListItemLayerType) {
        this.dialogListItemLayerType = dialogListItemLayerType;
        preferences.edit().putInt("dialogListItemLayerType", dialogListItemLayerType).commit();
    }

    public int getConversationListLayerType() {
        return conversationListLayerType;
    }

    public synchronized void setConversationListLayerType(int conversationListLayerType) {
        this.conversationListLayerType = conversationListLayerType;
        preferences.edit().putInt("conversationListLayerType", conversationListLayerType).commit();
    }

    public int getConversationListItemLayerType() {
        return conversationListItemLayerType;
    }

    public synchronized void setConversationListItemLayerType(int conversationListItemLayerType) {
        this.conversationListItemLayerType = conversationListItemLayerType;
        preferences.edit().putInt("conversationListItemLayerType", conversationListItemLayerType).commit();
    }

    public boolean isForceAnimations() {
        return forceAnimations;
    }

    public synchronized void setForceAnimations(boolean forceAnimations) {
        this.forceAnimations = forceAnimations;
        preferences.edit().putBoolean("forceAnimations", forceAnimations).commit();
    }

    public boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public synchronized void setDeveloperMode(boolean developerMode) {
        isDeveloperMode = developerMode;
        preferences.edit().putBoolean("isDeveloperMode", isDeveloperMode).commit();
    }

    public boolean isSaveLogs() {
        return isSaveLogs;
    }

    public synchronized void setSaveLogs(boolean saveLogs) {
        isSaveLogs = saveLogs;
        preferences.edit().putBoolean("isSaveLogs", isSaveLogs).commit();
    }
}
