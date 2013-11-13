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

    public UserSettings(Context context) {
        preferences = context.getSharedPreferences("org.telegram.android.UserSettings.pref", Context.MODE_PRIVATE);
        currentWallpaperId = preferences.getInt("wallpaperId", -1);
        isWallpaperSet = preferences.getBoolean("isWallpaperSet", false);
        currentWallpaperSolidColor = preferences.getInt("wallpaperColor", 0);
        isWallpaperSolid = preferences.getBoolean("isWallpaperSolid", false);
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
}
