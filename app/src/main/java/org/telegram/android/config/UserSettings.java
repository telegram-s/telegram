package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 18:02
 */
public class UserSettings {

    public static final int BAR_COLOR_DEFAULT = 0;
    public static final int BAR_COLOR_GREEN = 1;
    public static final int BAR_COLOR_RED = 2;
    public static final int BAR_COLOR_PURPLE = 3;
    public static final int BAR_COLOR_CYAN = 4;
    public static final int BAR_COLOR_WA = 5;
    public static final int BAR_COLOR_MAGENTA = 6;
    public static final int BAR_COLOR_LINE = 7;

    public static final int DIALOG_SIZE_NORMAL = 0;
    public static final int DIALOG_SIZE_LARGE = 1;

    public static final int PHOTO_SIZE_NORMAL = 0;
    public static final int PHOTO_SIZE_SMALL = 1;
    public static final int PHOTO_SIZE_LARGE = 2;

    public static final int BUBBLE_FONT_NORMAL = 0;
    public static final int BUBBLE_FONT_NORMAL_VALUE = 18;
    public static final int BUBBLE_FONT_NORMAL_VALUE_CLOCK = 12;
    public static final int BUBBLE_FONT_TINY = 1;
    public static final int BUBBLE_FONT_TINY_VALUE = 14;
    public static final int BUBBLE_FONT_TINY_VALUE_CLOCK = 12;
    public static final int BUBBLE_FONT_SMALL = 2;
    public static final int BUBBLE_FONT_SMALL_VALUE = 16;
    public static final int BUBBLE_FONT_SMALL_VALUE_CLOCK = 12;
    public static final int BUBBLE_FONT_LARGE = 3;
    public static final int BUBBLE_FONT_LARGE_VALUE = 20;
    public static final int BUBBLE_FONT_LARGE_VALUE_CLOCK = 16;
    public static final int BUBBLE_FONT_HUGE = 4;
    public static final int BUBBLE_FONT_HUGE_VALUE = 24;
    public static final int BUBBLE_FONT_HUGE_VALUE_CLOCK = 20;

    private SharedPreferences preferences;
    private int currentWallpaperId = -1;
    private boolean isWallpaperSet = false;
    private boolean isWallpaperSolid = false;
    private int currentWallpaperSolidColor = 0;
    private boolean isSaveToGalleryEnabled = true;
    private boolean showOnlyTelegramContacts = false;
    private int bubbleFontSize = BUBBLE_FONT_NORMAL;
    private boolean sendByEnter = false;
    private int dialogItemSize = DIALOG_SIZE_NORMAL;
    private int barColor = BAR_COLOR_DEFAULT;

    public UserSettings(Context context) {
        preferences = context.getSharedPreferences("org.telegram.android.UserSettings.pref", Context.MODE_PRIVATE);
        currentWallpaperId = preferences.getInt("wallpaperId", currentWallpaperId);
        isWallpaperSet = preferences.getBoolean("isWallpaperSet", isWallpaperSet);
        currentWallpaperSolidColor = preferences.getInt("wallpaperColor", currentWallpaperSolidColor);
        isWallpaperSolid = preferences.getBoolean("isWallpaperSolid", isWallpaperSolid);
        isSaveToGalleryEnabled = preferences.getBoolean("save_to_gallery", isSaveToGalleryEnabled);
        showOnlyTelegramContacts = preferences.getBoolean("only_telegram", showOnlyTelegramContacts);
        bubbleFontSize = preferences.getInt("bubbleFontSize", bubbleFontSize);
        sendByEnter = preferences.getBoolean("sendByEnter", sendByEnter);
        dialogItemSize = preferences.getInt("bubbleFontSize", dialogItemSize);
        barColor = preferences.getInt("barColor", BAR_COLOR_DEFAULT);
    }

    public int getBarColor() {
        return barColor;
    }

    public void setBarColor(int barColor) {
        this.barColor = barColor;
        preferences.edit().putInt("barColor", barColor).commit();
    }

    public int getCurrentWallpaperId() {
        return currentWallpaperId;
    }

    public void setCurrentWallpaperId(int currentWallpaperId) {
        this.currentWallpaperId = currentWallpaperId;
        preferences.edit().putInt("wallpaperId", currentWallpaperId).commit();
    }

    public boolean isWallpaperSet() {
        return isWallpaperSet;
    }

    public void setWallpaperSet(boolean wallpaperSet) {
        isWallpaperSet = wallpaperSet;
        preferences.edit().putBoolean("isWallpaperSet", wallpaperSet).commit();
    }

    public boolean isWallpaperSolid() {
        return isWallpaperSolid;
    }

    public void setWallpaperSolid(boolean wallpaperSolid) {
        isWallpaperSolid = wallpaperSolid;
        preferences.edit().putBoolean("isWallpaperSolid", isWallpaperSolid).commit();
    }

    public int getCurrentWallpaperSolidColor() {
        return currentWallpaperSolidColor;
    }

    public void setCurrentWallpaperSolidColor(int currentWallpaperSolidColor) {
        this.currentWallpaperSolidColor = currentWallpaperSolidColor;
        preferences.edit().putInt("wallpaperColor", currentWallpaperSolidColor).commit();
    }

    public boolean isSaveToGalleryEnabled() {
        return isSaveToGalleryEnabled;
    }

    public void setSaveToGalleryEnabled(boolean value) {
        isSaveToGalleryEnabled = value;
        preferences.edit().putBoolean("save_to_gallery", value).commit();
    }

    public int getBubbleFontSizeId() {
        return bubbleFontSize;
    }

    public void setBubbleFontSizeId(int bubbleFontSize) {
        this.bubbleFontSize = bubbleFontSize;
        preferences.edit().putInt("bubbleFontSize", bubbleFontSize).commit();
    }

    public int getDialogItemSize() {
        return dialogItemSize;
    }

    public void setDialogItemSize(int dialogItemSize) {
        this.dialogItemSize = dialogItemSize;
        preferences.edit().putInt("dialogItemSize", dialogItemSize).commit();
    }

    public boolean isSendByEnter() {
        return sendByEnter;
    }

    public void setSendByEnter(boolean sendByEnter) {
        this.sendByEnter = sendByEnter;
        preferences.edit().putBoolean("sendByEnter", sendByEnter).commit();
    }

    public void clearSettings() {
        String[] keys = preferences.getAll().keySet().toArray(new String[0]);
        SharedPreferences.Editor editor = preferences.edit();
        for (String k : keys) {
            editor.remove(k);
        }
        editor.commit();

        currentWallpaperId = -1;
        isWallpaperSet = false;
        isWallpaperSolid = false;
        currentWallpaperSolidColor = 0;
        isSaveToGalleryEnabled = true;
        showOnlyTelegramContacts = false;
        bubbleFontSize = BUBBLE_FONT_NORMAL;
        dialogItemSize = DIALOG_SIZE_NORMAL;
        sendByEnter = false;
        barColor = BAR_COLOR_DEFAULT;
    }

    public boolean showOnlyTelegramContacts() {
        return showOnlyTelegramContacts;
    }

    public void setShowOnlyTelegramContacts(boolean value) {
        showOnlyTelegramContacts = value;
        preferences.edit().putBoolean("only_telegram", value).commit();
    }
}
