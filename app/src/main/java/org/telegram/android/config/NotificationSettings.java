package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.android.TelegramApplication;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 12:32
 */
public class NotificationSettings {

    private static final String PREFERENCE_NAME = "org.telegram.android.Notifications";

    private static long ADD_TO_CONTACT_TIMEOUT = 24 * 60 * 60 * 1000;

    private SharedPreferences preferences;

    public NotificationSettings(TelegramApplication application) {
        this.preferences = application.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAddToContactVisible(int uid) {
        int sec = preferences.getInt("add_to_contact_" + uid, 0);
        if (System.currentTimeMillis() < sec * 1000L || sec * 1000L + ADD_TO_CONTACT_TIMEOUT < System.currentTimeMillis()) {
            return true;
        } else {
            return false;
        }
    }

    public void hideAddToContact(int uid) {
        preferences.edit().putInt("add_to_contact_" + uid, (int) (System.currentTimeMillis() / 1000)).commit();
    }

    public boolean isEnabled() {
        return preferences.getBoolean("enabled", true);
    }

    public void setEnabled(boolean value) {
        preferences.edit().putBoolean("enabled", value).commit();
    }

    public boolean isMessageSoundEnabled() {
        return preferences.getBoolean("enabled_sound", true);
    }

    public void setMessageSoundEnabled(boolean value) {
        preferences.edit().putBoolean("enabled_sound", value).commit();
    }

    public boolean isMessageVibrationEnabled() {
        return preferences.getBoolean("enabled_vibration", true);
    }

    public void setMessageVibrationEnabled(boolean value) {
        preferences.edit().putBoolean("enabled_vibration", value).commit();
    }

    public void setNotificationSound(String uri, String title) {
        preferences.edit()
                .putString("sound", uri)
                .putString("sound_title", title)
                .commit();
    }

    public String getNotificationSound() {
        return preferences.getString("sound", null);
    }

    public String getNotificationSoundTitle() {
        return preferences.getString("sound_title", null);
    }

    public boolean isGroupEnabled() {
        return preferences.getBoolean("enabled_group", true);
    }

    public void setGroupEnabled(boolean value) {
        preferences.edit().putBoolean("enabled_group", value).commit();
    }

    public boolean isGroupSoundEnabled() {
        return preferences.getBoolean("enabled_group_sound", true);
    }

    public void setGroupSoundEnabled(boolean value) {
        preferences.edit().putBoolean("enabled_group_sound", value).commit();
    }

    public boolean isGroupVibrateEnabled() {
        return preferences.getBoolean("enabled_group_vibrate", true);
    }

    public void setGroupVibrateEnabled(boolean value) {
        preferences.edit().putBoolean("enabled_group_vibrate", value).commit();
    }

    public void setNotificationGroupSound(String uri, String title) {
        preferences.edit()
                .putString("sound_group", uri)
                .putString("sound_group_title", title)
                .commit();
    }

    public String getNotificationGroupSound() {
        return preferences.getString("sound_group", null);
    }

    public String getNotificationSoundGroupTitle() {
        return preferences.getString("sound_group_title", null);
    }

    public boolean isInAppSoundsEnabled() {
        return preferences.getBoolean("in_app_sound", true);
    }

    public void setInAppSoundsEnabled(boolean value) {
        preferences.edit().putBoolean("in_app_sound", value).commit();
    }

    public boolean isInAppVibrateEnabled() {
        return preferences.getBoolean("in_app_vibrate", true);
    }

    public void setInAppVibrateEnabled(boolean value) {
        preferences.edit().putBoolean("in_app_vibrate", value).commit();
    }

    public boolean isInAppPreviewEnabled() {
        return preferences.getBoolean("in_app_preview", true);
    }

    public void setInAppPreviewEnabled(boolean value) {
        preferences.edit().putBoolean("in_app_preview", value).commit();
    }


    public boolean isEnabledForChat(int chatId) {
        return preferences.getBoolean("chat_" + chatId + "_enabled", true);
    }

    public boolean isEnabledForUser(int uid) {
        return preferences.getBoolean("uid_" + uid + "_enabled", true);
    }

    public String getUserNotificationSound(int uid) {
        return preferences.getString("uid_" + uid + "_sound", null);
    }

    public String getUserNotificationSoundTitle(int uid) {
        return preferences.getString("uid_" + uid + "_sound_title", null);
    }

    public void setUserNotificationSound(int uid, String uri, String title) {
        preferences.edit()
                .putString("uid_" + uid + "_sound", uri)
                .putString("uid_" + uid + "_sound_title", title)
                .commit();
    }

    public void disableForUser(int uid) {
        preferences.edit().putBoolean("uid_" + uid + "_enabled", false).commit();
    }

    public void enableForUser(int uid) {
        preferences.edit().putBoolean("uid_" + uid + "_enabled", true).commit();
    }

    public String getChatNotificationSound(int uid) {
        return preferences.getString("chat_" + uid + "_sound", null);
    }

    public String getChatNotificationSoundTitle(int uid) {
        return preferences.getString("chat_" + uid + "_sound_title", null);
    }

    public void setChatNotificationSound(int uid, String uri, String title) {
        preferences.edit()
                .putString("chat_" + uid + "_sound", uri)
                .putString("chat_" + uid + "_sound_title", title)
                .commit();
    }

    public void disableForChat(int uid) {
        preferences.edit().putBoolean("chat_" + uid + "_enabled", false).commit();
    }

    public void enableForChat(int uid) {
        preferences.edit().putBoolean("chat_" + uid + "_enabled", true).commit();
    }

    public void resetGroupSettings() {
        String[] keys = preferences.getAll().keySet().toArray(new String[0]);
        SharedPreferences.Editor editor = preferences.edit();
        for (String k : keys) {
            if (k.startsWith("chat_") || k.startsWith("uid_")) {
                editor.remove(k);
            }
        }
        editor.commit();
    }

    public void clearSettings() {
        String[] keys = preferences.getAll().keySet().toArray(new String[0]);
        SharedPreferences.Editor editor = preferences.edit();
        for (String k : keys) {
            editor.remove(k);
        }
        editor.commit();
    }
}
