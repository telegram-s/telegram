package org.telegram.android.config;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.telegram.android.StelsActivity;
import org.telegram.android.StelsApplication;

import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.09.13
 * Time: 18:35
 */
public class WallpaperHolder {
    private Bitmap wallpaper;
    private StelsApplication application;

    public WallpaperHolder(StelsApplication application) {
        this.application = application;
    }

    public Bitmap getBitmap() {
        if (wallpaper != null) {
            return wallpaper;
        }

        if (application.getUserSettings().isWallpaperSet()) {
            try {
                Bitmap w = BitmapFactory.decodeStream(application.openFileInput("current_wallpaper.jpg"));
                wallpaper = w;
                return wallpaper;
            } catch (FileNotFoundException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public void dropCache() {
        wallpaper = null;
    }
}
