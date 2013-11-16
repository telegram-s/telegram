package org.telegram.android.config;

import android.content.Context;
import android.content.SharedPreferences;
import com.extradea.framework.persistence.ContextPersistence;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 18:02
 */
public class UserSettings {
    private SharedPreferences preferences;
    private int currentWallpaperId = -1;
    private boolean isWallpaperSet = false;
    private boolean isWallpaperSolid = false;
    private int currentWallpaperSolidColor = 0;
    private boolean isSaveToGalleryEnabled = true;

    public UserSettings(Context context) {
        preferences = context.getSharedPreferences("org.telegram.android.UserSettings.pref", Context.MODE_PRIVATE);
        currentWallpaperId = preferences.getInt("wallpaperId", currentWallpaperId);
        isWallpaperSet = preferences.getBoolean("isWallpaperSet", isWallpaperSet);
        currentWallpaperSolidColor = preferences.getInt("wallpaperColor", currentWallpaperSolidColor);
        isWallpaperSolid = preferences.getBoolean("isWallpaperSolid", isWallpaperSolid);
        isSaveToGalleryEnabled = preferences.getBoolean("save_to_gallery", isSaveToGalleryEnabled);
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
    }
}
